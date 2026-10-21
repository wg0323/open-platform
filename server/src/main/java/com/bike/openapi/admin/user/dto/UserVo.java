package com.bike.openapi.admin.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户列表视图。
 */
@Data
public class UserVo {

    private Long id;
    private String username;
    private String nickname;
    private Boolean enabled;
    private Boolean mustChangePassword;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    private List<Long> roleIds;
    private List<String> roleNames;
}
