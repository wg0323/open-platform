package com.bike.openapi.admin.auth.dto;

import lombok.Data;
import com.bike.openapi.admin.auth.LoginUser;

import java.util.Set;

/**
 * 用户简要信息。
 */
@Data
public class UserBriefVo {

    private Long id;
    private String username;
    private String nickname;
    private Set<String> roleCodes;

    public static UserBriefVo of(LoginUser loginUser) {
        UserBriefVo vo = new UserBriefVo();
        vo.setId(loginUser.getUserId());
        vo.setUsername(loginUser.getUsername());
        vo.setNickname(loginUser.getNickname());
        vo.setRoleCodes(loginUser.getRoleCodes());
        return vo;
    }
}
