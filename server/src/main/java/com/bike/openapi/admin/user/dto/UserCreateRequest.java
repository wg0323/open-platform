package com.bike.openapi.admin.user.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 新建用户请求。
 */
@Data
public class UserCreateRequest {

    @NotBlank(message = "账号不能为空")
    @Size(min = 3, max = 32, message = "账号长度需为 3~32 位")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "账号只能包含字母、数字与下划线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度需为 8~32 位")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码需同时包含字母和数字")
    private String password;

    @Size(max = 64, message = "昵称最长 64 位")
    private String nickname;

    @NotEmpty(message = "至少选择一个角色")
    private List<Long> roleIds;
}
