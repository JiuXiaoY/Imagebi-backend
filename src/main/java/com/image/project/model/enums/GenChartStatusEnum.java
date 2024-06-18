package com.image.project.model.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author JXY
 * @version 1.0
 * @since 2024/6/8
 */
public enum GenChartStatusEnum {
    WAITING(0, "排队中"),
    RUNNING(1, "执行中"),
    SUCCESS(2, "成功"),
    FAILED(3, "失败");

    private final Integer status;
    private final String value;

    GenChartStatusEnum(Integer status, String value) {
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
     * @return
     */
    public static GenChartStatusEnum getEnumByValue(String value) {
        if (value == null) {
            return null;
        }
        for (GenChartStatusEnum anEnum : GenChartStatusEnum.values()) {
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
