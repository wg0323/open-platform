package com.bike.openapi.admin.auth.dto;

import lombok.Data;
import com.bike.openapi.admin.menu.vo.MenuNode;

import java.util.List;
import java.util.Set;

/**
 * 当前登录用户信息、权限码与菜单树。
 */
@Data
public class MeVo {

    private UserBriefVo user;
    private Set<String> perms;
    private List<MenuNode> menus;
    private boolean mustChangePassword;
}
