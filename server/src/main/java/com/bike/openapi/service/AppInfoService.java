package com.bike.openapi.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bike.openapi.domain.AppInfo;

/**
 * 接入方凭证服务。
 */
public interface AppInfoService extends IService<AppInfo> {

    /**
     * 按 appKey 查询凭证。
     *
     * @param appKey 接入方标识
     * @return 凭证，可能为 null
     */
    AppInfo getByAppKey(String appKey);

    /**
     * 新建接入方，appKey 与 appSecret 由服务端生成。
     *
     * @param companyCode 企业编码
     * @param companyName 企业名称
     * @param ipWhitelist IP 白名单
     * @param qps         每秒配额
     * @return 新建的凭证（含明文 secret）
     */
    AppInfo create(String companyCode, String companyName, String ipWhitelist, Integer qps);

    /**
     * 修改接入方配置，参数为 null 表示不修改。
     *
     * @param id          主键
     * @param companyName 企业名称
     * @param ipWhitelist IP 白名单
     * @param qps         每秒配额
     * @param enabled     是否启用
     * @return 修改后的凭证
     */
    AppInfo update(Long id, String companyName, String ipWhitelist, Integer qps, Boolean enabled);

    /**
     * 重置密钥。
     *
     * @param id 主键
     * @return 新的明文 secret
     */
    String resetSecret(Long id);

    /**
     * 查询明文密钥。
     *
     * @param id 主键
     * @return 明文 secret
     */
    String revealSecret(Long id);
}
