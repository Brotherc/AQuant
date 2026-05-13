package com.brotherc.aquant.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.brotherc.aquant.entity.SysUser;
import com.brotherc.aquant.exception.ExceptionEnum;
import com.brotherc.aquant.model.vo.auth.*;
import com.brotherc.aquant.repository.SysUserRepository;
import com.brotherc.aquant.utils.JwtUtils;
import com.brotherc.aquant.utils.SlidingWindowRateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int RESET_CODE_EXPIRE_MINUTES = 10;
    private static final int RESET_CODE_RESEND_INTERVAL_SECONDS = 60;

    /** 单 IP 1 分钟内最多 3 次发送验证码 */
    private static final SlidingWindowRateLimiter IP_LIMITER_PER_MINUTE =
            new SlidingWindowRateLimiter(Duration.ofMinutes(1), 3);
    /** 单 IP 1 小时内最多 10 次发送验证码 */
    private static final SlidingWindowRateLimiter IP_LIMITER_PER_HOUR =
            new SlidingWindowRateLimiter(Duration.ofHours(1), 10);
    /** 全局 1 天内最多 500 次发送（所有 IP 加起来） */
    private static final SlidingWindowRateLimiter GLOBAL_LIMITER_PER_DAY =
            new SlidingWindowRateLimiter(Duration.ofDays(1), 500);
    private static final String GLOBAL_LIMITER_KEY = "global";

    @Value("${spring.mail.username:}")
    private String mailFrom;

    private final SysUserRepository sysUserRepository;
    private final JavaMailSender mailSender;
    private final ConcurrentMap<String, ResetPasswordCodeCache> resetPasswordCodeCache = new ConcurrentHashMap<>();

    /**
     * 登录
     */
    public LoginRespVO login(LoginReqVO reqVO) {
        SysUser user = sysUserRepository.findByUsername(reqVO.getUsername())
                .orElseThrow(() -> ExceptionEnum.AUTH_LOGIN_FAILED.toException());

        if (user.getStatus() == 0) {
            throw ExceptionEnum.AUTH_ACCOUNT_DISABLED.toException();
        }

        BCrypt.Result result = BCrypt.verifyer().verify(reqVO.getPassword().toCharArray(), user.getPassword());
        if (!result.verified) {
            throw ExceptionEnum.AUTH_LOGIN_FAILED.toException();
        }

        String token = JwtUtils.generateToken(user.getId(), user.getUsername());

        LoginRespVO resp = new LoginRespVO();
        resp.setToken(token);
        resp.setNickname(user.getNickname() != null ? user.getNickname() : user.getUsername());
        resp.setUsername(user.getUsername());
        return resp;
    }

    /**
     * 注册
     */
    public void register(RegisterReqVO reqVO) {
        if (sysUserRepository.existsByUsername(reqVO.getUsername())) {
            throw ExceptionEnum.AUTH_USERNAME_EXISTS.toException();
        }

        SysUser user = new SysUser();
        user.setUsername(reqVO.getUsername());
        user.setPassword(BCrypt.withDefaults().hashToString(12, reqVO.getPassword().toCharArray()));
        user.setNickname(reqVO.getNickname());
        user.setEmail(normalizeOptionalEmail(reqVO.getEmail()));
        user.setStatus(1);
        sysUserRepository.save(user);
    }

    /**
     * 获取当前用户信息
     */
    public UserInfoVO getUserInfo(String token) {
        Long userId = JwtUtils.getUserId(token);
        if (userId == null) {
            throw ExceptionEnum.AUTH_TOKEN_INVALID.toException();
        }
        return getUserInfoById(userId);
    }

    /**
     * 根据用户 ID 获取用户信息
     */
    public UserInfoVO getUserInfoById(Long userId) {
        SysUser user = sysUserRepository.findById(userId)
                .orElseThrow(ExceptionEnum.AUTH_USER_NOT_FOUND::toException);

        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        return vo;
    }

    /**
     * 修改邮箱
     */
    public void updateEmail(Long userId, String newEmail) {
        if (userId == null) {
            throw ExceptionEnum.AUTH_TOKEN_INVALID.toException();
        }
        SysUser user = sysUserRepository.findById(userId)
                .orElseThrow(() -> ExceptionEnum.AUTH_USER_NOT_FOUND.toException());

        user.setEmail(normalizeRequiredEmail(newEmail));
        sysUserRepository.save(user);
    }

    public void sendResetPasswordCode(String email, String clientIp) {
        String normalizedEmail = normalizeRequiredEmail(email);

        if (mailFrom == null || mailFrom.isBlank()) {
            throw ExceptionEnum.AUTH_MAIL_NOT_CONFIGURED.toException();
        }

        // IP 限流：先消耗较严格的分钟级配额，再检查小时级
        String ipKey = clientIp == null || clientIp.isBlank() ? "unknown" : clientIp;
        if (!IP_LIMITER_PER_MINUTE.tryAcquire(ipKey) || !IP_LIMITER_PER_HOUR.tryAcquire(ipKey)) {
            log.warn("Reset code IP rate limited, ip={}, email={}", ipKey, normalizedEmail);
            throw ExceptionEnum.AUTH_RESET_CODE_IP_RATE_LIMIT.toException();
        }

        // 全局限流：防止成为邮件炸弹来源
        if (!GLOBAL_LIMITER_PER_DAY.tryAcquire(GLOBAL_LIMITER_KEY)) {
            log.warn("Reset code global rate limited, ip={}, email={}", ipKey, normalizedEmail);
            throw ExceptionEnum.AUTH_RESET_CODE_GLOBAL_RATE_LIMIT.toException();
        }

        // 同邮箱 60 秒限频（避免通过"过快报错/成功"区分邮箱是否存在）
        LocalDateTime now = LocalDateTime.now();
        ResetPasswordCodeCache existing = resetPasswordCodeCache.get(normalizedEmail);
        if (existing != null && existing.nextSendAllowedAt().isAfter(now)) {
            throw ExceptionEnum.AUTH_RESET_CODE_SEND_TOO_FREQUENT.toException();
        }

        // 邮箱不存在：静默返回，避免邮箱枚举
        SysUser user = sysUserRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null) {
            log.warn("Reset code requested for non-existent email: {}", normalizedEmail);
            return;
        }

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        resetPasswordCodeCache.put(normalizedEmail, new ResetPasswordCodeCache(
                code,
                now.plusMinutes(RESET_CODE_EXPIRE_MINUTES),
                now.plusSeconds(RESET_CODE_RESEND_INTERVAL_SECONDS)
        ));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(normalizedEmail);
        message.setSubject("AQuant 找回密码验证码");
        message.setText(buildResetPasswordMailContent(user, code));
        try {
            mailSender.send(message);
        } catch (MailException e) {
            resetPasswordCodeCache.remove(normalizedEmail);
            throw ExceptionEnum.AUTH_RESET_CODE_SEND_FAILED.toException(e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPasswordByEmail(ResetPasswordReqVO reqVO) {
        String normalizedEmail = normalizeRequiredEmail(reqVO.getEmail());

        // 先做验证码校验，再查邮箱：即使邮箱不存在也统一按"验证码不正确"返回，避免枚举
        ResetPasswordCodeCache cache = resetPasswordCodeCache.get(normalizedEmail);
        if (cache == null || !cache.code().equals(reqVO.getCode().trim())) {
            throw ExceptionEnum.AUTH_RESET_CODE_INVALID.toException();
        }
        if (cache.expiredAt().isBefore(LocalDateTime.now())) {
            resetPasswordCodeCache.remove(normalizedEmail);
            throw ExceptionEnum.AUTH_RESET_CODE_EXPIRED.toException();
        }

        SysUser user = sysUserRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> ExceptionEnum.AUTH_RESET_CODE_INVALID.toException());

        BCrypt.Result result = BCrypt.verifyer().verify(reqVO.getNewPassword().toCharArray(), user.getPassword());
        if (result.verified) {
            throw ExceptionEnum.AUTH_RESET_PASSWORD_SAME_AS_OLD.toException();
        }

        user.setPassword(BCrypt.withDefaults().hashToString(12, reqVO.getNewPassword().toCharArray()));
        sysUserRepository.save(user);
        resetPasswordCodeCache.remove(normalizedEmail);
    }

    private String buildResetPasswordMailContent(SysUser user, String code) {
        String displayName = user.getNickname() != null && !user.getNickname().isBlank()
                ? user.getNickname()
                : user.getUsername();
        return "您好，" + displayName + "：\n\n"
                + "您正在使用 AQuant 的找回密码功能。\n"
                + "本次验证码为：" + code + "\n"
                + "验证码 " + RESET_CODE_EXPIRE_MINUTES + " 分钟内有效，请勿泄露给他人。\n\n"
                + "如果这不是您的操作，请忽略此邮件。";
    }

    private String normalizeRequiredEmail(String email) {
        return email == null ? null : email.trim();
    }

    private String normalizeOptionalEmail(String email) {
        if (email == null) {
            return null;
        }
        String normalizedEmail = email.trim();
        return normalizedEmail.isEmpty() ? null : normalizedEmail;
    }

    private record ResetPasswordCodeCache(
            String code,
            LocalDateTime expiredAt,
            LocalDateTime nextSendAllowedAt
    ) {
    }

}
