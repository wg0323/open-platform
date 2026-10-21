package com.bike.openapi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.bike.openapi.domain.SysMenu;

import java.util.Collection;
import java.util.List;

/**
 * 管理后台菜单 Mapper。
 */
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 查询若干角色的权限码并去重。
     *
     * @param roleIds 角色主键集合
     * @return 权限码
     */
    @Select("<script>SELECT DISTINCT m.perms FROM sys_menu m "
            + "JOIN sys_role_menu rm ON rm.menu_id = m.id "
            + "WHERE rm.role_id IN "
            + "<foreach open='(' separator=',' close=')' collection='roleIds' item='id'>#{id}</foreach> "
            + "AND m.perms IS NOT NULL AND m.perms != ''</script>")
    List<String> selectPermsByRoleIds(@Param("roleIds") Collection<Long> roleIds);

    /**
     * 查询全部权限码，供超级管理员使用。
     *
     * @return 权限码
     */
    @Select("SELECT DISTINCT perms FROM sys_menu WHERE perms IS NOT NULL AND perms != ''")
    List<String> selectAllPerms();

    /**
     * 查询角色已授权的菜单主键。
     *
     * @param roleId 角色主键
     * @return 菜单主键
     */
    @Select("SELECT menu_id FROM sys_role_menu WHERE role_id = #{roleId}")
    List<Long> selectIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 查询若干角色已授权的菜单主键。
     *
     * @param roleIds 角色主键集合
     * @return 菜单主键
     */
    @Select("<script>SELECT m.id FROM sys_menu m "
            + "JOIN sys_role_menu rm ON rm.menu_id = m.id "
            + "WHERE rm.role_id IN "
            + "<foreach open='(' separator=',' close=')' collection='roleIds' item='id'>#{id}</foreach></script>")
    List<Long> selectIdsByRoleIds(@Param("roleIds") Collection<Long> roleIds);
}
