package com.image.project.model.dto.chartmessage;

import com.image.project.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author JXY
 * @version 1.0
 * @since 2024/6/14
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChartMessageQueryRequest extends PageRequest implements Serializable {
    /**
     * 接收信息用户
     */
    private Long receiverId;

    private static final long serialVersionUID = 1L;
}
