package com.bike.openapi.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bike.openapi.common.BizException;
import com.bike.openapi.common.ErrorCode;
import com.bike.openapi.domain.AppInfo;
import com.bike.openapi.mapper.AppInfoMapper;
import com.bike.openapi.service.AppInfoService;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * 接入方凭证服务实现。
 */
@Service
public class AppInfoServiceImpl extends ServiceImpl<AppInfoMapper, AppInfo> implements AppInfoService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SECRET_BYTES = 32;
    private static final String APP_KEY_PREFIX = "sbike_";
    private static final String UNDERLINE = "_";
    private static final int SUFFIX_BOUND = 1000;

    @Override
    public AppInfo getByAppKey(String appKey) {
        return getOne(Wrappers.<AppInfo>lambdaQuery().eq(AppInfo::getAppKey, appKey), false);
    }

    @Override
    public AppInfo create(String companyCode, String companyName, String ipWhitelist, Integer qps) {
        AppInfo appInfo = new AppInfo();
        appInfo.setAppKey(nextAppKey(companyCode));
        appInfo.setAppSecret(randomSecret());
        appInfo.setCompanyCode(companyCode);
        appInfo.setCompanyName(companyName);
        appInfo.setIpWhitelist(ipWhitelist);
        appInfo.setQps(qps == null ? 10 : qps);
        appInfo.setEnabled(Boolean.TRUE);
        save(appInfo);
        return appInfo;
    }

    @Override
    public AppInfo update(Long id, String companyName, String ipWhitelist, Integer qps, Boolean enabled) {
        AppInfo appInfo = requireById(id);
        if (companyName != null) {
            appInfo.setCompanyName(companyName);
        }
        if (ipWhitelist != null) {
            appInfo.setIpWhitelist(ipWhitelist);
        }
        if (qps != null) {
            appInfo.setQps(qps);
        }
        if (enabled != null) {
            appInfo.setEnabled(enabled);
        }
        updateById(appInfo);
        return appInfo;
    }

    @Override
    public String resetSecret(Long id) {
        AppInfo appInfo = requireById(id);
        String secret = randomSecret();
        appInfo.setAppSecret(secret);
        updateById(appInfo);
        return secret;
    }

    @Override
    public String revealSecret(Long id) {
        return requireById(id).getAppSecret();
    }

    private AppInfo requireById(Long id) {
        AppInfo appInfo = getById(id);
        if (appInfo == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "接入方不存在");
        }
        return appInfo;
    }

    private String nextAppKey(String companyCode) {
        String candidate = APP_KEY_PREFIX + companyCode;
        if (getByAppKey(candidate) == null) {
            return candidate;
        }
        return candidate + UNDERLINE + RANDOM.nextInt(SUFFIX_BOUND);
    }

    private String randomSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
