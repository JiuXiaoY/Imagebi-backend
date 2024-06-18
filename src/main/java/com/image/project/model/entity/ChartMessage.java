package com.image.project.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 消息通知表
 * @TableName chart_message
 */
@TableName(value ="chart_message")
@Data
public class ChartMessage implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 发送信息用户
     */
    private Long senderId;

    /**
     * 接收信息用户
     */
    private Long receiverId;

    /**
     * 图表Id
     */
    private Long chartId;

    /**
     * 通知内容
     */
    private String messageContent;

    /**
     * 通知类型
     */
    private Integer messageType;

    /**
     * 通知状态
     */
    private Integer messageStatus;

    /**
     * 通知时间
     */
    private Date messageTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 删除状态
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}