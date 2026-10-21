package com.bike.openapi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.bike.openapi.domain.Area;

/**
 * 区域视图。
 */
@Data
@Schema(description = "区域信息")
public class AreaVo {

    @Schema(description = "区域唯一标识")
    private Long id;

    @Schema(description = "区域编码")
    private String code;

    @Schema(description = "区域名称")
    private String name;

    @Schema(description = "上级区域编码")
    private String parentCode;

    @Schema(description = "区域围栏经纬度 JSON 串")
    private String fenceRange;

    public static AreaVo from(Area area) {
        AreaVo vo = new AreaVo();
        vo.setId(area.getId());
        vo.setCode(area.getCode());
        vo.setName(area.getName());
        vo.setParentCode(area.getParentCode());
        vo.setFenceRange(area.getFenceRange());
        return vo;
    }
}
