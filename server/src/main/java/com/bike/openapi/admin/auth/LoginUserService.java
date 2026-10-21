package com.bike.openapi.admin.auth;

import lombok.RequiredArgsConstructor;
import com.bike.openapi.domain.SysRole;
import com.bike.openapi.domain.SysUser;
import com.bike.openapi.mapper.SysMenuMapper;
import com.bike.openapi.mapper.SysRoleMapper;
import com.bike.openapi.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 按用户装配角色与权限码。每次请求实时查库，权限变更立即生效。
 */
@Service
@RequiredArgsConstructor
public class LoginUserService {

    /** 超级管理员角色编码。 */
    public static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;

    /**
     * 按主键装配登录主体。
     *
     * @param userId 用户主键
     * @return 登录主体，用户不存在返回 null
     */
    public LoginUser loadByUserId(Long userId) {
        return build(userMapper.selectById(userId));
    }

    /**
     * 按账号装配登录主体。
     *
     * @param username 登录账号
     * @return 登录主体，用户不存在返回 null
     */
    public LoginUser loadByUsername(String username) {
        return build(userMapper.selectByUsername(username));
    }

    public boolean isSuperAdmin(LoginUser loginUser) {
        return loginUser.getRoleCodes().contains(SUPER_ADMIN);
    }

    private LoginUser build(SysUser user) {
        if (user == null) {
            return null;
        }
        List<SysRole> roles = roleMapper.selectEnabledByUserId(user.getId());
        Set<String> roleCodes = roles.stream().map(SysRole::getRoleCode).collect(Collectors.toSet());
        Set<String> perms;
        if (roleCodes.contains(SUPER_ADMIN)) {
            perms = new HashSet<>(menuMapper.selectAllPerms());
        } else if (roles.isEmpty()) {
            perms = Collections.emptySet();
        } else {
            List<Long> roleIds = roles.stream().map(SysRole::getId).collect(Collectors.toList());
            perms = new HashSet<>(menuMapper.selectPermsByRoleIds(roleIds));
        }
        return new LoginUser(user.getId(), user.getUsername(), user.getPassword(), user.getNickname(),
                Boolean.TRUE.equals(user.getEnabled()), Boolean.TRUE.equals(user.getMustChangePassword()),
                user.getTokenVersion() == null ? 0 : user.getTokenVersion(), roleCodes, perms);
    }
}
