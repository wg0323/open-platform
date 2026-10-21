package com.bike.openapi.api.v3;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import com.bike.openapi.common.ApiResult;
import com.bike.openapi.domain.Area;
import com.bike.openapi.service.AreaService;
import com.bike.openapi.vo.AreaVo;
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
 * 区域下发接口。
 */
@Tag(name = "area", description = "区域")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v3/areas")
public class AreaController {

    private final AreaService areaService;

    /**
     * 分页拉取区域。
     *
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @param areaId   区域标识
     * @return 区域分页结果
     */
    @Operation(summary = "分页拉取区域", operationId = "listAreas")
    @GetMapping
    public ApiResult<PageResult<AreaVo>> page(@RequestParam(defaultValue = "1") @Min(1) long pageNo,
                                              @RequestParam(defaultValue = "100") @Min(1) @Max(500) long pageSize,
                                              @RequestParam(required = false) Long areaId) {
        IPage<Area> page = areaService.pageAreas(pageNo, pageSize, areaId);
        List<AreaVo> rows = page.getRecords().stream().map(AreaVo::from).collect(Collectors.toList());
        return ApiResult.ok(PageResult.of(page.getTotal(), rows));
    }
}
