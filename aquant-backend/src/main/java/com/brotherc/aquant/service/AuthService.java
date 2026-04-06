package com.brotherc.aquant.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.brotherc.aquant.entity.SysUser;
import com.brotherc.aquant.exception.ExceptionEnum;
import com.brotherc.aquant.model.vo.auth.*;
import com.brotherc.aquant.repository.SysUserRepository;
import com.brotherc.aquant.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

/**
 * 认证服务
 */
@Service
@RequiredArgsConstructor
public class AuthService implements CommandLineRunner {

    private final SysUserRepository sysUserRepository;

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
        user.setEmail(reqVO.getEmail());
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

        user.setEmail(newEmail);
        sysUserRepository.save(user);
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

}
