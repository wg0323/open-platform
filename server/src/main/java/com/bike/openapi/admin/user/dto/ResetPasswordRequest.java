package com.bike.openapi.admin.user.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 重置他人密码请求。
 */
@Data
public class ResetPasswordRequest {

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度需为 8~32 位")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码需同时包含字母和数字")
    private String password;
}
