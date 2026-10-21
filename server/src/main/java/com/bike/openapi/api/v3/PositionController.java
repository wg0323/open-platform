package com.bike.openapi.api.v3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import com.bike.openapi.common.ApiResult;
import com.bike.openapi.dto.PositionCreateRequest;
import com.bike.openapi.security.ApiAuthFilter;
import com.bike.openapi.service.BikePositionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 位置上传接口。
 */
@Tag(name = "position", description = "位置")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v3/positions")
public class PositionController {

    private final BikePositionService bikePositionService;

    /**
     * 上传位置，按（appKey, sn, positionTime）幂等覆盖。
     *
     * @param request        位置请求
     * @param servletRequest 原始请求，用于取 appKey
     * @return 空数据
     */
    @Operation(summary = "上传位置", operationId = "uploadPosition")
    @PostMapping
    public ApiResult<Void> create(@Valid @RequestBody PositionCreateRequest request,
                                  HttpServletRequest servletRequest) {
        String appKey = (String) servletRequest.getAttribute(ApiAuthFilter.ATTR_APP_KEY);
        bikePositionService.savePosition(appKey, request);
        return ApiResult.ok(null);
    }
}
