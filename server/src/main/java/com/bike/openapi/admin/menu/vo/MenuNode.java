package com.bike.openapi.admin.menu.vo;

import lombok.Data;
import com.bike.openapi.domain.SysMenu;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树节点，目录与菜单带 children，按钮无 children。
 */
@Data
public class MenuNode {

    /** 目录。 */
    public static final int TYPE_DIRECTORY = 0;
    /** 菜单。 */
    public static final int TYPE_MENU = 1;
    /** 按钮。 */
    public static final int TYPE_BUTTON = 2;

    private Long id;
    private Long parentId;
    private String menuName;
    private Integer menuType;
    private String path;
    private String component;
    private String perms;
    private String icon;
    private Integer sortNo;
    private List<MenuNode> children = new ArrayList<>();

    public static MenuNode of(SysMenu menu) {
        MenuNode node = new MenuNode();
        node.setId(menu.getId());
        node.setParentId(menu.getParentId());
        node.setMenuName(menu.getMenuName());
        node.setMenuType(menu.getMenuType());
        node.setPath(menu.getPath());
        node.setComponent(menu.getComponent());
        node.setPerms(menu.getPerms());
        node.setIcon(menu.getIcon());
        node.setSortNo(menu.getSortNo());
        return node;
    }
}
