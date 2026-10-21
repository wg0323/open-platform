package com.bike.openapi.admin.menu;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import com.bike.openapi.admin.auth.LoginUser;
import com.bike.openapi.admin.auth.LoginUserService;
import com.bike.openapi.admin.menu.vo.MenuNode;
import com.bike.openapi.domain.SysMenu;
import com.bike.openapi.mapper.SysMenuMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 菜单树装配与角色授权。
 */
@Service
@RequiredArgsConstructor
public class SysMenuService {

    private final SysMenuMapper menuMapper;
    private final LoginUserService loginUserService;

    /**
     * 当前用户可见的菜单树：超管全量，普通用户按权限码过滤后剪掉空目录。
     *
     * @param loginUser 登录主体
     * @return 菜单树
     */
    public List<MenuNode> treeOf(LoginUser loginUser) {
        List<SysMenu> menus = menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                .in(SysMenu::getMenuType, MenuNode.TYPE_DIRECTORY, MenuNode.TYPE_MENU)
                .eq(SysMenu::getVisible, true)
                .orderByAsc(SysMenu::getSortNo)
                .orderByAsc(SysMenu::getId));
        if (!loginUserService.isSuperAdmin(loginUser)) {
            Set<String> perms = loginUser.getPerms();
            menus = menus.stream()
                    .filter(menu -> menu.getPerms() == null || menu.getPerms().isEmpty()
                            || perms.contains(menu.getPerms()))
                    .collect(Collectors.toList());
        }
        List<MenuNode> roots = assemble(menus);
        roots.forEach(this::pruneEmptyDirectory);
        roots.removeIf(node -> Objects.equals(node.getMenuType(), MenuNode.TYPE_DIRECTORY)
                && node.getChildren().isEmpty());
        return roots;
    }

    /**
     * 全量菜单树，供角色授权勾选。
     *
     * @return 菜单树
     */
    public List<MenuNode> fullTree() {
        List<SysMenu> menus = menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                .orderByAsc(SysMenu::getSortNo)
                .orderByAsc(SysMenu::getId));
        return assemble(menus);
    }

    private List<MenuNode> assemble(List<SysMenu> menus) {
        Map<Long, MenuNode> nodes = new LinkedHashMap<>();
        for (SysMenu menu : menus) {
            nodes.put(menu.getId(), MenuNode.of(menu));
        }
        List<MenuNode> roots = new ArrayList<>();
        for (MenuNode node : nodes.values()) {
            MenuNode parent = nodes.get(node.getParentId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    private void pruneEmptyDirectory(MenuNode node) {
        if (node.getChildren().isEmpty()) {
            return;
        }
        node.getChildren().forEach(this::pruneEmptyDirectory);
        node.getChildren().removeIf(child -> Objects.equals(child.getMenuType(), MenuNode.TYPE_DIRECTORY)
                && child.getChildren().isEmpty());
    }
}
