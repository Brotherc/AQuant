package com.brotherc.aquant.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.brotherc.aquant.entity.SysUser;
import com.brotherc.aquant.exception.ExceptionEnum;
import com.brotherc.aquant.model.vo.auth.*;
import com.brotherc.aquant.repository.SysUserRepository;
import com.brotherc.aquant.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 认证服务
 */
@Service
@RequiredArgsConstructor
public class AuthService implements CommandLineRunner {

    private static final int RESET_CODE_EXPIRE_MINUTES = 10;
    private static final int RESET_CODE_RESEND_INTERVAL_SECONDS = 60;

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

        SysUser user = sysUserRepository.findById(userId)
                .orElseThrow(() -> ExceptionEnum.AUTH_USER_NOT_FOUND.toException());

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

    public void sendResetPasswordCode(String email) {
        String normalizedEmail = normalizeRequiredEmail(email);
        SysUser user = sysUserRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> ExceptionEnum.AUTH_EMAIL_NOT_FOUND.toException());

        if (mailFrom == null || mailFrom.isBlank()) {
            throw ExceptionEnum.AUTH_MAIL_NOT_CONFIGURED.toException();
        }

        LocalDateTime now = LocalDateTime.now();
        ResetPasswordCodeCache existing = resetPasswordCodeCache.get(normalizedEmail);
        if (existing != null && existing.nextSendAllowedAt().isAfter(now)) {
            throw ExceptionEnum.AUTH_RESET_CODE_SEND_TOO_FREQUENT.toException();
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
        SysUser user = sysUserRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> ExceptionEnum.AUTH_EMAIL_NOT_FOUND.toException());

        ResetPasswordCodeCache cache = resetPasswordCodeCache.get(normalizedEmail);
        if (cache == null || !cache.code().equals(reqVO.getCode().trim())) {
            throw ExceptionEnum.AUTH_RESET_CODE_INVALID.toException();
        }
        if (cache.expiredAt().isBefore(LocalDateTime.now())) {
            resetPasswordCodeCache.remove(normalizedEmail);
            throw ExceptionEnum.AUTH_RESET_CODE_EXPIRED.toException();
        }

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

    /**
     * 应用启动时初始化默认管理员账号
     */
    @Override
    public void run(String... args) {
        if (!sysUserRepository.existsByUsername("admin")) {
            SysUser admin = new SysUser();
            admin.setUsername("admin");
            admin.setPassword(BCrypt.withDefaults().hashToString(12, "admin123".toCharArray()));
            admin.setNickname("管理员");
            admin.setStatus(1);
            sysUserRepository.save(admin);
            System.out.println("[AQuant] 默认管理员账号已创建: admin / admin123");
        }
    }

    private record ResetPasswordCodeCache(
            String code,
            LocalDateTime expiredAt,
            LocalDateTime nextSendAllowedAt
    ) {
    }

}
