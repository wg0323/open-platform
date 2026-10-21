package com.bike.openapi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.bike.openapi.domain.SysRole;

import java.util.List;

/**
 * 管理后台角色 Mapper。
 */
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 查询用户拥有的启用角色。
     *
     * @param userId 用户主键
     * @return 角色列表
     */
    @Select("SELECT r.* FROM sys_role r JOIN sys_user_role ur ON ur.role_id = r.id "
            + "WHERE ur.user_id = #{userId} AND r.enabled = 1")
    List<SysRole> selectEnabledByUserId(@Param("userId") Long userId);

    /**
     * 查询用户的全部角色，含已停用，用于回显。
     *
     * @param userId 用户主键
     * @return 角色列表
     */
    @Select("SELECT r.* FROM sys_role r JOIN sys_user_role ur ON ur.role_id = r.id "
            + "WHERE ur.user_id = #{userId}")
    List<SysRole> selectByUserId(@Param("userId") Long userId);

    /**
     * 统计引用该角色的用户数。
     *
     * @param roleId 角色主键
     * @return 用户数
     */
    @Select("SELECT COUNT(*) FROM sys_user_role WHERE role_id = #{roleId}")
    long countUsers(@Param("roleId") Long roleId);
}
