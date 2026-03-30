package com.winter.fund.modules.auth.model;

/**
 * 认证模块 DTO 定义文件，负责集中声明接口入参与出参模型。
 */

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public final class AuthDtos {

    private AuthDtos() {
    }

    @Schema(description = "登录请求")
    public record LoginRequest(
        @Schema(description = "用户名，仅支持字母、数字和下划线", example = "demo_analyst")
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_]{3,32}$", message = "username is invalid") String username,
        @Schema(description = "登录密码", example = "ChangeMe123!")
        @NotBlank @Size(min = 6, max = 100) String password
    ) {
    }

    @Schema(description = "注册请求")
    public record RegisterRequest(
        @Schema(description = "展示名称", example = "Winter")
        @NotBlank @Size(min = 2, max = 30) String displayName,
        @Schema(description = "用户名", example = "winter_user")
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_]{3,32}$", message = "username is invalid") String username,
        @Schema(description = "登录密码", example = "ChangeMe123!")
        @NotBlank @Size(min = 6, max = 100) String password
    ) {
    }

    @Schema(description = "刷新令牌请求")
    public record RefreshRequest(
        @Schema(description = "刷新令牌", example = "fc_rt_a18c4f77-acde-48d4-bfdd-4d40f96c3b2d")
        @NotBlank String refreshToken
    ) {
    }

    @Schema(description = "当前用户信息")
    public record UserProfileResponse(
        @Schema(description = "用户 ID", example = "user-demo-001") String id,
        @Schema(description = "展示名称", example = "Demo Analyst") String displayName,
        @Schema(description = "用户名", example = "demo_analyst") String username,
        @Schema(description = "风险模式", example = "research") String riskMode
    ) {
    }

    @Schema(description = "登录或刷新后的鉴权结果")
    public record AuthResponse(
        @Schema(description = "访问令牌", example = "fc_at_6e3a2fa1-14fb-4f58-8cc4-5bba8fe6c114") String accessToken,
        @Schema(description = "刷新令牌", example = "fc_rt_6d319df6-7f42-43f2-9a89-d07d408dc8fa") String refreshToken,
        @Schema(description = "访问令牌剩余秒数", example = "14400") long expiresIn,
        @Schema(description = "当前用户资料") UserProfileResponse profile
    ) {
    }
}
