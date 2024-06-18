package com.image.project.model.dto.chart;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;

/**
 * @author JXY
 * @version 1.0
 * @since 2024/6/4
 */
@Data
public class GenChartByAiRequest implements Serializable {

    /**
     * 相关名字
     */
    private String relatedName;

    /**
     * 分析目标
     */
    private String goal;

    /**
     * 图表类型
     */
    private String chartType;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
