package com.bike.openapi.admin.user;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import com.bike.openapi.admin.auth.LoginUser;
import com.bike.openapi.admin.user.dto.AssignRolesRequest;
import com.bike.openapi.admin.user.dto.ResetPasswordRequest;
import com.bike.openapi.admin.user.dto.UserCreateRequest;
import com.bike.openapi.admin.user.dto.UserUpdateRequest;
import com.bike.openapi.admin.user.dto.UserVo;
import com.bike.openapi.common.ApiResult;
import com.bike.openapi.vo.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 管理台：用户管理。
 */
@Hidden
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/api/users")
public class SysUserController {

    private final SysUserService sysUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:user:list')")
    public ApiResult<PageResult<UserVo>> page(@RequestParam(defaultValue = "1") int pageNo,
                                              @RequestParam(defaultValue = "10") int pageSize,
                                              @RequestParam(required = false) String keyword) {
        return ApiResult.ok(sysUserService.page(pageNo, pageSize, keyword));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:user:create')")
    public ApiResult<Void> create(@Valid @RequestBody UserCreateRequest request) {
        sysUserService.create(request);
        return ApiResult.ok(null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:update')")
    public ApiResult<Void> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request,
                                  @AuthenticationPrincipal LoginUser loginUser) {
        sysUserService.update(id, request, loginUser.getUserId());
        return ApiResult.ok(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:delete')")
    public ApiResult<Void> delete(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        sysUserService.delete(id, loginUser.getUserId());
        return ApiResult.ok(null);
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("hasAuthority('system:user:reset-password')")
    public ApiResult<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        sysUserService.resetPassword(id, request.getPassword());
        return ApiResult.ok(null);
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('system:user:assign-role')")
    public ApiResult<Void> assignRoles(@PathVariable Long id, @RequestBody AssignRolesRequest request) {
        sysUserService.assignRoles(id, request.getRoleIds());
        return ApiResult.ok(null);
    }
}
