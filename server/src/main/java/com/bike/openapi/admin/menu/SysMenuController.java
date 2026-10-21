package com.bike.openapi.admin.menu;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import com.bike.openapi.admin.menu.vo.MenuNode;
import com.bike.openapi.common.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理台：菜单只读接口，供角色授权。
 */
@Hidden
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/api/menus")
public class SysMenuController {

    private final SysMenuService sysMenuService;

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('system:role:assign-menu')")
    public ApiResult<List<MenuNode>> tree() {
        return ApiResult.ok(sysMenuService.fullTree());
    }
}
