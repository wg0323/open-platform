package com.bike.openapi.admin.role.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 新建角色请求。
 */
@Data
public class RoleCreateRequest {

    @NotBlank(message = "角色编码不能为空")
    @Size(max = 64, message = "角色编码最长 64 位")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "角色编码需以大写字母开头，只含大写字母、数字与下划线")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64, message = "角色名称最长 64 位")
    private String roleName;

    @Size(max = 255, message = "备注最长 255 位")
    private String remark;
}
