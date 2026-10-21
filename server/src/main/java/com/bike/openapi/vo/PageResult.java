package com.bike.openapi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 分页结果。
 */
@Data
@Schema(description = "分页结果")
public class PageResult<T> {

    @Schema(description = "总条数")
    private long total;

    @Schema(description = "当前页数据")
    private List<T> rows;

    public static <T> PageResult<T> of(long total, List<T> rows) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(total);
        result.setRows(rows);
        return result;
    }
}
