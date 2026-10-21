package com.bike.openapi.admin.user.dto;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 编辑用户请求。
 */
@Data
public class UserUpdateRequest {

    @Size(max = 64, message = "昵称最长 64 位")
    private String nickname;

    private Boolean enabled;
}
