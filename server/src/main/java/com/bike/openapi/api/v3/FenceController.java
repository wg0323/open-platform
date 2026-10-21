package com.bike.openapi.api.v3;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import com.bike.openapi.common.ApiResult;
import com.bike.openapi.domain.Fence;
import com.bike.openapi.service.FenceService;
import com.bike.openapi.vo.FenceVo;
import com.bike.openapi.vo.PageResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 围栏下发接口。
 */
@Tag(name = "fence", description = "围栏")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v3/fences")
public class FenceController {

    private final FenceService fenceService;

    /**
     * 分页拉取围栏。
     *
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @param fenceId  围栏标识
     * @return 围栏分页结果
     */
    @Operation(summary = "分页拉取围栏", operationId = "listFences")
    @GetMapping
    public ApiResult<PageResult<FenceVo>> page(@RequestParam(defaultValue = "1") @Min(1) long pageNo,
                                               @RequestParam(defaultValue = "100") @Min(1) @Max(500) long pageSize,
                                               @RequestParam(required = false) Long fenceId) {
        IPage<Fence> page = fenceService.pageFences(pageNo, pageSize, fenceId);
        List<FenceVo> rows = page.getRecords().stream().map(FenceVo::from).collect(Collectors.toList());
        return ApiResult.ok(PageResult.of(page.getTotal(), rows));
    }
}
