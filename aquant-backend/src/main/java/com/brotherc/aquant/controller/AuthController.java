package com.brotherc.aquant.controller;

import com.brotherc.aquant.model.dto.common.ResponseDTO;
import com.brotherc.aquant.model.vo.auth.*;
import com.brotherc.aquant.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证管理")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "登录")
    @PostMapping("/login")
    public ResponseDTO<LoginRespVO> login(@RequestBody @Valid LoginReqVO reqVO) {
        return ResponseDTO.success(authService.login(reqVO));
    }

    @Operation(summary = "注册")
    @PostMapping("/register")
    public ResponseDTO<Void> register(@RequestBody @Valid RegisterReqVO reqVO) {
        authService.register(reqVO);
        return ResponseDTO.success();
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/userInfo")
    public ResponseDTO<UserInfoVO> getUserInfo(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = authHeader.substring(7);
        return ResponseDTO.success(authService.getUserInfo(token));
    }

    @Operation(summary = "修改邮箱")
    @PostMapping("/updateEmail")
    public ResponseDTO<Void> updateEmail(
            @RequestBody @Valid UpdateEmailReqVO reqVO,
            @RequestAttribute(value = "userId", required = false) Long userId
    ) {
        authService.updateEmail(userId, reqVO.getEmail());
        return ResponseDTO.success();
    }

    @Operation(summary = "发送找回密码验证码")
    @PostMapping("/password/sendResetCode")
    public ResponseDTO<Void> sendResetCode(@RequestBody @Valid SendResetCodeReqVO reqVO) {
        authService.sendResetPasswordCode(reqVO.getEmail());
        return ResponseDTO.success();
    }

    @Operation(summary = "通过邮箱重置密码")
    @PostMapping("/password/reset")
    public ResponseDTO<Void> resetPassword(@RequestBody @Valid ResetPasswordReqVO reqVO) {
        authService.resetPasswordByEmail(reqVO);
        return ResponseDTO.success();
    }

}
