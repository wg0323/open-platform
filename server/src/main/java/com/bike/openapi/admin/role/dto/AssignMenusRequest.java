package com.bike.openapi.admin.role.dto;

import lombok.Data;

import java.util.List;

/**
 * 分配菜单权限请求，菜单主键由前端补齐父节点。
 */
@Data
public class AssignMenusRequest {

    private List<Long> menuIds;
}
