package com.image.project.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author JXY
 * @version 1.0
 * @since 2024/6/5
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BiResponse {
    /**
     * 生成图表数据
     */
    private String genChart;

    /**
     * 生成图表分析结果
     */
    private String genResult;

    /**
     * 图标id
     */
    private Long chartId;
}
