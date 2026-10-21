package com.bike.openapi.admin.role;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import com.bike.openapi.admin.auth.LoginUserService;
import com.bike.openapi.admin.role.dto.RoleCreateRequest;
import com.bike.openapi.admin.role.dto.RoleUpdateRequest;
import com.bike.openapi.admin.role.dto.RoleVo;
import com.bike.openapi.common.BizException;
import com.bike.openapi.common.ErrorCode;
import com.bike.openapi.domain.SysRole;
import com.bike.openapi.domain.SysRoleMenu;
import com.bike.openapi.mapper.SysMenuMapper;
import com.bike.openapi.mapper.SysRoleMapper;
import com.bike.openapi.mapper.SysRoleMenuMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理后台角色服务。
 */
@Service
@RequiredArgsConstructor
public class SysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;

    /**
     * 角色列表，含引用用户数与已授权菜单。
     *
     * @return 角色列表
     */
    public List<RoleVo> list() {
        return roleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                        .orderByAsc(SysRole::getId)).stream()
                .map(this::toVo).collect(Collectors.toList());
    }

    /**
     * 新建角色。
     *
     * @param request 新建请求
     */
    public void create(RoleCreateRequest request) {
        Long exists = roleMapper.selectCount(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getRoleCode, request.getRoleCode()));
        if (exists != null && exists > 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "角色编码已存在");
        }
        SysRole role = new SysRole();
        role.setRoleCode(request.getRoleCode());
        role.setRoleName(request.getRoleName());
        role.setRemark(request.getRemark());
        role.setEnabled(Boolean.TRUE);
        roleMapper.insert(role);
    }

    /**
     * 修改角色名称、备注与启停状态。
     *
     * @param id      角色主键
     * @param request 修改请求
     */
    public void update(Long id, RoleUpdateRequest request) {
        SysRole role = requireRole(id);
        role.setRoleName(request.getRoleName() == null ? role.getRoleName() : request.getRoleName());
        role.setRemark(request.getRemark() == null ? role.getRemark() : request.getRemark());
        role.setEnabled(request.getEnabled() == null ? role.getEnabled() : request.getEnabled());
        roleMapper.updateById(role);
    }

    /**
     * 删除角色。超级管理员角色与被引用的角色不可删除。
     *
     * @param id 角色主键
     */
    public void delete(Long id) {
        SysRole role = requireRole(id);
        if (LoginUserService.SUPER_ADMIN.equals(role.getRoleCode())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "超级管理员角色不可删除");
        }
        long users = roleMapper.countUsers(id);
        if (users > 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "该角色仍有 " + users + " 个用户引用，不能删除");
        }
        roleMapper.deleteById(id);
    }

    /**
     * 覆盖写入角色菜单授权。
     *
     * @param id      角色主键
     * @param menuIds 菜单主键，含父目录
     */
    public void assignMenus(Long id, List<Long> menuIds) {
        requireRole(id);
        roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, id));
        if (menuIds == null) {
            return;
        }
        for (Long menuId : menuIds) {
            SysRoleMenu relation = new SysRoleMenu();
            relation.setRoleId(id);
            relation.setMenuId(menuId);
            roleMenuMapper.insert(relation);
        }
    }

    private SysRole requireRole(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "角色不存在");
        }
        return role;
    }

    private RoleVo toVo(SysRole role) {
        RoleVo vo = new RoleVo();
        vo.setId(role.getId());
        vo.setRoleCode(role.getRoleCode());
        vo.setRoleName(role.getRoleName());
        vo.setRemark(role.getRemark());
        vo.setEnabled(role.getEnabled());
        vo.setUserCount(roleMapper.countUsers(role.getId()));
        vo.setMenuIds(menuMapper.selectIdsByRoleId(role.getId()));
        return vo;
    }
}
