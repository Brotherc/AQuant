package com.brotherc.aquant.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.brotherc.aquant.entity.SysUser;
import com.brotherc.aquant.exception.ExceptionEnum;
import com.brotherc.aquant.model.vo.auth.*;
import com.brotherc.aquant.repository.SysUserRepository;
import com.brotherc.aquant.utils.JwtUtils;
import com.brotherc.aquant.utils.SlidingWindowRateLimiter;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
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

    /** 登录 IP 限流：单 IP 1 分钟最多 10 次 */
    private static final SlidingWindowRateLimiter LOGIN_IP_LIMITER_PER_MINUTE =
            new SlidingWindowRateLimiter(Duration.ofMinutes(1), 10);
    /** 登录 IP 限流：单 IP 1 小时最多 30 次失败请求 */
    private static final SlidingWindowRateLimiter LOGIN_IP_FAIL_LIMITER_PER_HOUR =
            new SlidingWindowRateLimiter(Duration.ofHours(1), 30);

    /** 账号锁定阈值：30 分钟内连续失败 5 次则锁定 */
    private static final int LOGIN_FAIL_THRESHOLD = 5;
    private static final Duration LOGIN_FAIL_WINDOW = Duration.ofMinutes(30);
    private static final Duration LOGIN_LOCK_DURATION = Duration.ofMinutes(15);

    @Value("${spring.mail.username:}")
    private String mailFrom;

    private final SysUserRepository sysUserRepository;
    private final JavaMailSender mailSender;
    /** 验证码缓存，自动按写入时间过期 */
    private final Cache<String, ResetPasswordCodeCache> resetPasswordCodeCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(RESET_CODE_EXPIRE_MINUTES))
            .maximumSize(10_000)
            .build();

    /** 用户名 → 失败次数；统计窗口内的连续失败 */
    private final Cache<String, Integer> loginFailCounter = Caffeine.newBuilder()
            .expireAfterWrite(LOGIN_FAIL_WINDOW)
            .maximumSize(10_000)
            .build();

    /** 用户名 → 解锁时间；锁定期内禁止登录 */
    private final Cache<String, LocalDateTime> loginLockUntil = Caffeine.newBuilder()
            .expireAfterWrite(LOGIN_LOCK_DURATION)
            .maximumSize(10_000)
            .build();

    /**
     * 登录
     *
     * @param reqVO    登录请求
     * @param clientIp 客户端 IP（用于限流）
     */
    public LoginRespVO login(LoginReqVO reqVO, String clientIp) {
        String ipKey = clientIp == null || clientIp.isBlank() ? "unknown" : clientIp;
        String username = reqVO.getUsername();

        // 1. IP 限流：单 IP 每分钟最多 10 次登录请求
        if (!LOGIN_IP_LIMITER_PER_MINUTE.tryAcquire(ipKey)) {
            log.warn("Login IP rate limited (per-minute), ip={}, username={}", ipKey, username);
            throw ExceptionEnum.AUTH_LOGIN_IP_RATE_LIMIT.toException();
        }

        // 2. 账号锁定检查
        LocalDateTime unlockAt = loginLockUntil.getIfPresent(username);
        if (unlockAt != null && unlockAt.isAfter(LocalDateTime.now())) {
            long minutesLeft = Math.max(1L,
                    java.time.Duration.between(LocalDateTime.now(), unlockAt).toMinutes() + 1);
            throw ExceptionEnum.AUTH_ACCOUNT_LOCKED.toFormattedException(minutesLeft);
        }

        // 3. 校验用户名密码
        SysUser user = sysUserRepository.findByUsername(username).orElse(null);
        if (user == null) {
            // 用户名不存在：消耗 IP 失败计数，但不计入用户名锁定（避免攻击者锁别人账号）
            recordIpLoginFailure(ipKey);
            throw ExceptionEnum.AUTH_LOGIN_FAILED.toException();
        }

        if (user.getStatus() == 0) {
            throw ExceptionEnum.AUTH_ACCOUNT_DISABLED.toException();
        }

        BCrypt.Result result = BCrypt.verifyer().verify(reqVO.getPassword().toCharArray(), user.getPassword());
        if (!result.verified) {
            recordIpLoginFailure(ipKey);
            recordUserLoginFailure(username);
            throw ExceptionEnum.AUTH_LOGIN_FAILED.toException();
        }

        // 4. 登录成功：清除该用户的失败计数与锁定状态
        loginFailCounter.invalidate(username);
        loginLockUntil.invalidate(username);

        String token = JwtUtils.generateToken(user.getId(), user.getUsername());

        LoginRespVO resp = new LoginRespVO();
        resp.setToken(token);
        resp.setNickname(user.getNickname() != null ? user.getNickname() : user.getUsername());
        resp.setUsername(user.getUsername());
        return resp;
    }

    /**
     * 记录某用户名的登录失败，达到阈值后写入锁定时间
     */
    private void recordUserLoginFailure(String username) {
        Integer count = loginFailCounter.get(username, k -> 0);
        count = (count == null ? 0 : count) + 1;
        loginFailCounter.put(username, count);

        if (count >= LOGIN_FAIL_THRESHOLD) {
            loginLockUntil.put(username, LocalDateTime.now().plus(LOGIN_LOCK_DURATION));
            loginFailCounter.invalidate(username);
            log.warn("Account locked due to too many failed logins, username={}", username);
        }
    }

    /**
     * 记录登录失败的 IP，超过 IP 失败上限直接拒绝（不影响合法用户的下一次成功）
     */
    private void recordIpLoginFailure(String ipKey) {
        if (!LOGIN_IP_FAIL_LIMITER_PER_HOUR.tryAcquire(ipKey)) {
            log.warn("Login IP rate limited (per-hour failures), ip={}", ipKey);
            throw ExceptionEnum.AUTH_LOGIN_IP_RATE_LIMIT.toException();
        }
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
        ResetPasswordCodeCache existing = resetPasswordCodeCache.getIfPresent(normalizedEmail);
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
            resetPasswordCodeCache.invalidate(normalizedEmail);
            throw ExceptionEnum.AUTH_RESET_CODE_SEND_FAILED.toException(e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPasswordByEmail(ResetPasswordReqVO reqVO) {
        String normalizedEmail = normalizeRequiredEmail(reqVO.getEmail());

        // 先做验证码校验，再查邮箱：即使邮箱不存在也统一按"验证码不正确"返回，避免枚举
        ResetPasswordCodeCache cache = resetPasswordCodeCache.getIfPresent(normalizedEmail);
        if (cache == null || !cache.code().equals(reqVO.getCode().trim())) {
            throw ExceptionEnum.AUTH_RESET_CODE_INVALID.toException();
        }
        if (cache.expiredAt().isBefore(LocalDateTime.now())) {
            resetPasswordCodeCache.invalidate(normalizedEmail);
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
        resetPasswordCodeCache.invalidate(normalizedEmail);
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
