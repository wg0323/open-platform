package com.bike.openapi.admin.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 验证码响应。
 */
@Data
@AllArgsConstructor
public class CaptchaVo {

    private String captchaId;
    private String image;
}
