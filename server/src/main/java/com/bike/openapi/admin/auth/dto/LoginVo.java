package com.bike.openapi.admin.auth.dto;

import lombok.Data;
import com.bike.openapi.admin.auth.LoginUser;

/**
 * 登录响应。
 */
@Data
public class LoginVo {

    private String token;
    private boolean mustChangePassword;
    private UserBriefVo user;

    public static LoginVo of(String token, LoginUser loginUser) {
        LoginVo vo = new LoginVo();
        vo.setToken(token);
        vo.setMustChangePassword(loginUser.isMustChangePassword());
        vo.setUser(UserBriefVo.of(loginUser));
        return vo;
    }
}
