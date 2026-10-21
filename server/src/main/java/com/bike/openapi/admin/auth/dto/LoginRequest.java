package com.bike.openapi.admin.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 登录请求。
 */
@Data
public class LoginRequest {

    @NotBlank(message = "验证码标识不能为空")
    private String captchaId;

    @NotBlank(message = "验证码不能为空")
    private String captchaCode;

    @NotBlank(message = "账号不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
