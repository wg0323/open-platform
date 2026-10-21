package com.bike.openapi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.bike.openapi.domain.SysUser;

/**
 * 管理后台用户 Mapper。
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 按账号查询。
     *
     * @param username 登录账号
     * @return 用户
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username} LIMIT 1")
    SysUser selectByUsername(@Param("username") String username);

    /**
     * 统计启用中的超级管理员数量，用于保护最后一个超管。
     *
     * @return 数量
     */
    @Select("SELECT COUNT(DISTINCT u.id) FROM sys_user u "
            + "JOIN sys_user_role ur ON ur.user_id = u.id "
            + "JOIN sys_role r ON r.id = ur.role_id "
            + "WHERE r.role_code = 'SUPER_ADMIN' AND r.enabled = 1 AND u.enabled = 1")
    long countEnabledSuperAdmins();
}
