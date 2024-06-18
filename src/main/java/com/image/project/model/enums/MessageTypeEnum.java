package com.image.project.model.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author JXY
 * @version 1.0
 * @since 2024/6/8
 */
public enum MessageTypeEnum {
    SYSTEM_MESSAGE(0, "系统消息"),
    MY_NEWS(1, "我的消息"),
    PERSONAL_LETTER(2, "私信");

    private final Integer status;
    private final String value;

    MessageTypeEnum(Integer status, String value) {
        this.status = status;
        this.value = value;
    }

    /**
     * 获取值列表
     *
     * @return
     */
    public static List<String> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    /**
     * 根据value获取枚举
     *
     * @return
     */
    public static MessageTypeEnum getEnumByValue(String value) {
        if (value == null) {
            return null;
        }
        for (MessageTypeEnum anEnum : MessageTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

    public String getValue() {
        return value;
    }

    public Integer getStatus() {
        return status;
    }
}
