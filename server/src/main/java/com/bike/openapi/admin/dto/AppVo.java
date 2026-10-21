package com.bike.openapi.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.bike.openapi.domain.AppInfo;

import java.time.LocalDateTime;

/**
 * 接入方视图，列表中默认打码密钥。
 */
@Data
@Schema(description = "接入方信息")
public class AppVo {

    private static final String MASK = "******";
    private static final int VISIBLE_TAIL = 4;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "接入方标识")
    private String appKey;

    @Schema(description = "密钥，默认打码")
    private String appSecret;

    @Schema(description = "企业编码")
    private String companyCode;

    @Schema(description = "企业名称")
    private String companyName;

    @Schema(description = "IP 白名单")
    private String ipWhitelist;

    @Schema(description = "每秒配额")
    private Integer qps;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 列表视图，密钥打码。
     *
     * @param appInfo 凭证
     * @return 视图
     */
    public static AppVo masked(AppInfo appInfo) {
        AppVo vo = of(appInfo);
        String secret = appInfo.getAppSecret();
        if (secret != null && secret.length() > VISIBLE_TAIL) {
            vo.setAppSecret(MASK + secret.substring(secret.length() - VISIBLE_TAIL));
        }
        return vo;
    }

    /**
     * 新建或重置后返回明文密钥。
     *
     * @param appInfo 凭证
     * @return 视图
     */
    public static AppVo revealed(AppInfo appInfo) {
        return of(appInfo);
    }

    private static AppVo of(AppInfo appInfo) {
        AppVo vo = new AppVo();
        vo.setId(appInfo.getId());
        vo.setAppKey(appInfo.getAppKey());
        vo.setAppSecret(appInfo.getAppSecret());
        vo.setCompanyCode(appInfo.getCompanyCode());
        vo.setCompanyName(appInfo.getCompanyName());
        vo.setIpWhitelist(appInfo.getIpWhitelist());
        vo.setQps(appInfo.getQps());
        vo.setEnabled(appInfo.getEnabled());
        vo.setCreateTime(appInfo.getCreateTime());
        return vo;
    }
}
