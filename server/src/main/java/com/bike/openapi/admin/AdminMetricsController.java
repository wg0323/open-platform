package com.bike.openapi.admin;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import com.bike.openapi.admin.dto.MetricHistoryPointVo;
import com.bike.openapi.common.ApiResult;
import com.bike.openapi.metrics.ApiMetricsRecorder;
import com.bike.openapi.metrics.MetricSnapshot;
import com.bike.openapi.service.MetricHistoryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理台：按 appKey 的指标查询。
 */
@Hidden
@Tag(name = "管理台-指标")
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/api/metrics")
public class AdminMetricsController {

    private final ApiMetricsRecorder apiMetricsRecorder;
    private final MetricHistoryService metricHistoryService;

    /**
     * 查询所有 appKey 的窗口指标。
     *
     * @return 指标快照
     */
    @Operation(summary = "指标快照")
    @PreAuthorize("hasAuthority('metrics:view')")
    @GetMapping
    public ApiResult<List<MetricSnapshot>> metrics() {
        return ApiResult.ok(apiMetricsRecorder.snapshotAll());
    }

    /**
     * 按 appKey 查询分钟级指标历史。
     *
     * @param appKey 接入方标识
     * @param start  起始时间，为空时取结束时间前 1 小时
     * @param end    结束时间，为空时取当前时间
     * @return 分钟级历史点
     */
    @Operation(summary = "指标历史")
    @PreAuthorize("hasAuthority('metrics:view')")
    @GetMapping("/history")
    public ApiResult<List<MetricHistoryPointVo>> history(
            @RequestParam String appKey,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) {
        List<MetricHistoryPointVo> points = metricHistoryService.query(appKey, start, end).stream()
                .map(MetricHistoryPointVo::of)
                .collect(Collectors.toList());
        return ApiResult.ok(points);
    }
}
