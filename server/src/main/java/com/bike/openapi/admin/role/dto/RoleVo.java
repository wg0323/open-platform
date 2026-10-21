package com.bike.openapi.admin.role.dto;

import lombok.Data;

import java.util.List;

/**
 * 角色视图。
 */
@Data
public class RoleVo {

    private Long id;
    private String roleCode;
    private String roleName;
    private String remark;
    private Boolean enabled;
    private Long userCount;
    private List<Long> menuIds;
}
