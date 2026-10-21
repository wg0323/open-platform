package com.bike.openapi.admin.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import com.bike.openapi.admin.auth.LoginUserService;
import com.bike.openapi.admin.user.dto.UserCreateRequest;
import com.bike.openapi.admin.user.dto.UserUpdateRequest;
import com.bike.openapi.admin.user.dto.UserVo;
import com.bike.openapi.common.BizException;
import com.bike.openapi.common.ErrorCode;
import com.bike.openapi.domain.SysRole;
import com.bike.openapi.domain.SysUser;
import com.bike.openapi.domain.SysUserRole;
import com.bike.openapi.mapper.SysRoleMapper;
import com.bike.openapi.mapper.SysUserMapper;
import com.bike.openapi.mapper.SysUserRoleMapper;
import com.bike.openapi.vo.PageResult;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理后台用户服务。
 */
@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 分页查询用户。
     *
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @param keyword  账号或昵称关键字
     * @return 分页结果
     */
    public PageResult<UserVo> page(int pageNo, int pageSize, String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.<SysUser>lambdaQuery()
                .orderByDesc(SysUser::getId);
        if (keyword != null && !keyword.trim().isEmpty()) {
            String value = keyword.trim();
            wrapper.and(item -> item.like(SysUser::getUsername, value).or().like(SysUser::getNickname, value));
        }
        Page<SysUser> page = userMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        List<UserVo> rows = page.getRecords().stream().map(this::toVo).collect(Collectors.toList());
        return PageResult.of(page.getTotal(), rows);
    }

    /**
     * 新建用户，初始密码标记为需强制修改。
     *
     * @param request 新建请求
     */
    public void create(UserCreateRequest request) {
        if (userMapper.selectByUsername(request.getUsername()) != null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "账号已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() == null ? request.getUsername() : request.getNickname());
        user.setEnabled(Boolean.TRUE);
        user.setMustChangePassword(Boolean.TRUE);
        user.setTokenVersion(0);
        userMapper.insert(user);
        replaceRoles(user.getId(), request.getRoleIds());
    }

    /**
     * 修改昵称与启停状态。
     *
     * @param id            用户主键
     * @param request       修改请求
     * @param currentUserId 当前登录用户
     */
    public void update(Long id, UserUpdateRequest request, Long currentUserId) {
        SysUser user = requireUser(id);
        boolean disabling = Boolean.FALSE.equals(request.getEnabled());
        if (disabling && id.equals(currentUserId)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "不能停用当前登录账号");
        }
        if (disabling && holdsSuperAdmin(id) && userMapper.countEnabledSuperAdmins() <= 1) {
            throw new BizException(ErrorCode.PARAM_INVALID, "不能停用最后一个超级管理员");
        }
        user.setNickname(request.getNickname() == null ? user.getNickname() : request.getNickname());
        user.setEnabled(request.getEnabled() == null ? user.getEnabled() : request.getEnabled());
        userMapper.updateById(user);
    }

    /**
     * 删除用户，同时清理角色关联。
     *
     * @param id            用户主键
     * @param currentUserId 当前登录用户
     */
    public void delete(Long id, Long currentUserId) {
        if (id.equals(currentUserId)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "不能删除当前登录账号");
        }
        requireUser(id);
        if (holdsSuperAdmin(id) && userMapper.countEnabledSuperAdmins() <= 1) {
            throw new BizException(ErrorCode.PARAM_INVALID, "不能删除最后一个超级管理员");
        }
        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, id));
        userMapper.deleteById(id);
    }

    /**
     * 管理员重置他人密码，置强制改密并使旧 token 失效。
     *
     * @param id       用户主键
     * @param password 新密码明文
     */
    public void resetPassword(Long id, String password) {
        SysUser user = requireUser(id);
        user.setPassword(passwordEncoder.encode(password));
        user.setMustChangePassword(Boolean.TRUE);
        user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
        userMapper.updateById(user);
    }

    /**
     * 重新分配角色。
     *
     * @param id      用户主键
     * @param roleIds 角色主键
     */
    public void assignRoles(Long id, List<Long> roleIds) {
        requireUser(id);
        replaceRoles(id, roleIds == null ? Collections.emptyList() : roleIds);
    }

    private void replaceRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId));
        for (Long roleId : roleIds) {
            SysUserRole relation = new SysUserRole();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            userRoleMapper.insert(relation);
        }
    }

    private boolean holdsSuperAdmin(Long userId) {
        return roleMapper.selectByUserId(userId).stream()
                .anyMatch(role -> LoginUserService.SUPER_ADMIN.equals(role.getRoleCode()));
    }

    private SysUser requireUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "用户不存在");
        }
        return user;
    }

    private UserVo toVo(SysUser user) {
        UserVo vo = new UserVo();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEnabled(user.getEnabled());
        vo.setMustChangePassword(user.getMustChangePassword());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setCreateTime(user.getCreateTime());
        List<SysRole> roles = roleMapper.selectByUserId(user.getId());
        vo.setRoleIds(roles.stream().map(SysRole::getId).collect(Collectors.toList()));
        vo.setRoleNames(roles.stream().map(SysRole::getRoleName).collect(Collectors.toList()));
        return vo;
    }
}
