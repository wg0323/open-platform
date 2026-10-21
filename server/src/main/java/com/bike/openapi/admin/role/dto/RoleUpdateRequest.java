package com.bike.openapi.admin.role.dto;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 编辑角色请求。
 */
@Data
public class RoleUpdateRequest {

    @Size(max = 64, message = "角色名称最长 64 位")
    private String roleName;

    @Size(max = 255, message = "备注最长 255 位")
    private String remark;

    private Boolean enabled;
}
