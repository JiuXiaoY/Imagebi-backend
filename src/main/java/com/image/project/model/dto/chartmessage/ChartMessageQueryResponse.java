package com.image.project.model.dto.chartmessage;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author JXY
 * @version 1.0
 * @since 2024/6/14
 */

@Data
public class ChartMessageQueryResponse implements Serializable {
    /**
     * 消息id
     */
    private Long id;

    /**
     * 用户头像
     */
    private String avatarUrl;

    /**
     * 图表名称
     */
    private String relatedName;

    /**
     * 消息内容
     */
    private String messageContent;

    /**
     * 消息状态
     */
    private Integer messageStatus;

    /**
     * 消息时间
     */
    private Date messageTime;

    /**
     * 消息类型
     */
    private Integer messageType;

    private static final long serialVersionUID = 1L;
}
