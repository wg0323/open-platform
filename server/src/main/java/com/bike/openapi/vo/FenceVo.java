package com.bike.openapi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.bike.openapi.domain.Fence;

/**
 * 围栏视图。
 */
@Data
@Schema(description = "围栏信息")
public class FenceVo {

    @Schema(description = "围栏唯一标识")
    private Long id;

    @Schema(description = "所属区域编码")
    private String areaCode;

    @Schema(description = "围栏名称")
    private String name;

    @Schema(description = "真实围栏经纬度 JSON 串")
    private String fence;

    @Schema(description = "逻辑围栏经纬度 JSON 串")
    private String fenceLogic;

    @Schema(description = "围栏自行车上限")
    private Integer limits;

    @Schema(description = "围栏类型：0 禁停，1 可停")
    private Integer fenceType;

    @Schema(description = "围栏备注")
    private String memo;

    public static FenceVo from(Fence fence) {
        FenceVo vo = new FenceVo();
        vo.setId(fence.getId());
        vo.setAreaCode(fence.getAreaCode());
        vo.setName(fence.getName());
        vo.setFence(fence.getFence());
        vo.setFenceLogic(fence.getFenceLogic());
        vo.setLimits(fence.getLimits());
        vo.setFenceType(fence.getFenceType());
        vo.setMemo(fence.getMemo());
        return vo;
    }
}
