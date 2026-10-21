package com.bike.openapi.admin.user.dto;

import lombok.Data;

import java.util.List;

/**
 * 分配角色请求。
 */
@Data
public class AssignRolesRequest {

    private List<Long> roleIds;
}
