package com.bike.openapi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.bike.openapi.domain.SysUserRole;

import java.util.List;

/**
 * 用户角色关联 Mapper。
 */
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    /**
     * 查询用户已分配的角色主键。
     *
     * @param userId 用户主键
     * @return 角色主键
     */
    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);
}
