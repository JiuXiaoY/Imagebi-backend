package com.image.project.utils;

import com.image.project.exception.ThrowUtils;
import com.image.project.manager.AiManager;

import javax.annotation.Resource;

/**
 * @author JXY
 * @version 1.0
 * @since 2024/6/17
 */
public class ValidEChartJsonFormatUtils {


    @Resource
    private AiManager aiManager;

    /**
     * 基础校验 （有待完善）
     *
     * @param json
     * @return
     */
    public static boolean isValidEChartJsonFormat(String json) {
        // 验证json格式
        if (!json.startsWith("{") || !json.endsWith("}")) {
            return false;
        }
        // todo 待完善
        return true;
    }

    /**
     * 利用 Ai 校验修正 Ai 生成的图表代码
     *
     * @param userId 用户id
     * @param genChart 图表
     * @return
     */
    public String AiAmendResult(Long userId, String genChart) {
        String amendResult = aiManager.doChatSpark(userId, genChart);
        ThrowUtils.throwIf(amendResult == null, new RuntimeException("amendResult is null"));
        return amendResult;
    }
}
