package com.bike.openapi.admin.role;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import com.bike.openapi.admin.role.dto.AssignMenusRequest;
import com.bike.openapi.admin.role.dto.RoleCreateRequest;
import com.bike.openapi.admin.role.dto.RoleUpdateRequest;
import com.bike.openapi.admin.role.dto.RoleVo;
import com.bike.openapi.common.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 管理台：角色管理。
 */
@Hidden
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/api/roles")
public class SysRoleController {

    private final SysRoleService sysRoleService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:role:list')")
    public ApiResult<List<RoleVo>> list() {
        return ApiResult.ok(sysRoleService.list());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:role:create')")
    public ApiResult<Void> create(@Valid @RequestBody RoleCreateRequest request) {
        sysRoleService.create(request);
        return ApiResult.ok(null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:update')")
    public ApiResult<Void> update(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        sysRoleService.update(id, request);
        return ApiResult.ok(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:delete')")
    public ApiResult<Void> delete(@PathVariable Long id) {
        sysRoleService.delete(id);
        return ApiResult.ok(null);
    }

    @PutMapping("/{id}/menus")
    @PreAuthorize("hasAuthority('system:role:assign-menu')")
    public ApiResult<Void> assignMenus(@PathVariable Long id, @RequestBody AssignMenusRequest request) {
        sysRoleService.assignMenus(id, request.getMenuIds());
        return ApiResult.ok(null);
    }
}
