package com.winter.fund.modules.auth;

/**
 * 认证模块控制器，负责对外暴露该模块的 HTTP 接口。
 */

import com.winter.fund.modules.auth.model.AuthDtos;
import com.winter.fund.modules.auth.model.CurrentUser;
import com.winter.fund.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "认证相关接口")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "注册", description = "创建新用户并返回登录态。")
    public AuthDtos.AuthResponse register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "登录", description = "使用用户名密码登录并返回访问令牌和刷新令牌。")
    public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌", description = "使用刷新令牌换取新的访问令牌。")
    public AuthDtos.AuthResponse refresh(@Valid @RequestBody AuthDtos.RefreshRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "退出登录", description = "撤销当前访问令牌。")
    public void logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        authService.logout(authorizationHeader);
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户", description = "返回当前登录用户的资料信息。")
    public AuthDtos.UserProfileResponse me(@AuthenticationPrincipal CurrentUser currentUser) {
        return authService.me(currentUser);
    }
}
