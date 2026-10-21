package com.bike.openapi.admin;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import com.bike.openapi.admin.dto.AppCreateRequest;
import com.bike.openapi.admin.dto.AppUpdateRequest;
import com.bike.openapi.admin.dto.AppVo;
import com.bike.openapi.common.ApiResult;
import com.bike.openapi.domain.AppInfo;
import com.bike.openapi.security.AppCredentialService;
import com.bike.openapi.service.AppInfoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理台：接入方 CRUD。
 */
@Hidden
@Tag(name = "管理台-接入方")
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/api/apps")
public class AdminAppController {

    private final AppInfoService appInfoService;
    private final AppCredentialService appCredentialService;

    /**
     * 查询接入方列表，密钥打码。
     *
     * @return 接入方列表
     */
    @Operation(summary = "接入方列表")
    @PreAuthorize("hasAuthority('app:list')")
    @GetMapping
    public ApiResult<List<AppVo>> list() {
        List<AppVo> apps = appInfoService.list().stream()
                .map(AppVo::masked)
                .collect(Collectors.toList());
        return ApiResult.ok(apps);
    }

    /**
     * 新建接入方，appKey 与 appSecret 由服务端生成。
     *
     * @param request 新建请求
     * @return 含明文密钥的接入方
     */
    @Operation(summary = "新建接入方")
    @PreAuthorize("hasAuthority('app:create')")
    @PostMapping
    public ApiResult<AppVo> create(@Valid @RequestBody AppCreateRequest request) {
        AppInfo appInfo = appInfoService.create(request.getCompanyCode(), request.getCompanyName(),
                request.getIpWhitelist(), request.getQps());
        appCredentialService.invalidateAll();
        return ApiResult.ok(AppVo.revealed(appInfo));
    }

    /**
     * 修改接入方配置。
     *
     * @param id      主键
     * @param request 修改请求
     * @return 修改后的接入方
     */
    @Operation(summary = "修改接入方")
    @PreAuthorize("hasAuthority('app:update')")
    @PutMapping("/{id}")
    public ApiResult<AppVo> update(@PathVariable Long id, @Valid @RequestBody AppUpdateRequest request) {
        AppInfo appInfo = appInfoService.update(id, request.getCompanyName(), request.getIpWhitelist(),
                request.getQps(), request.getEnabled());
        appCredentialService.invalidateAll();
        return ApiResult.ok(AppVo.masked(appInfo));
    }

    /**
     * 重置密钥。
     *
     * @param id 主键
     * @return 新的明文密钥
     */
    @Operation(summary = "重置密钥")
    @PreAuthorize("hasAuthority('app:reset-secret')")
    @PostMapping("/{id}/reset-secret")
    public ApiResult<String> resetSecret(@PathVariable Long id) {
        String secret = appInfoService.resetSecret(id);
        appCredentialService.invalidateAll();
        return ApiResult.ok(secret);
    }

    /**
     * 查看明文密钥，仅用于测试环境交付。
     *
     * @param id 主键
     * @return 明文密钥
     */
    @Operation(summary = "查看明文密钥")
    @PreAuthorize("hasAuthority('app:reveal-secret')")
    @GetMapping("/{id}/secret")
    public ApiResult<String> revealSecret(@PathVariable Long id) {
        return ApiResult.ok(appInfoService.revealSecret(id));
    }
}
