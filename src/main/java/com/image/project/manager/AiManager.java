package com.image.project.manager;

import com.image.AiAccess.dev.client.ImageSparkClient;
import com.image.AiAccess.dev.client.ImageYuCongMingClient;
import com.image.AiAccess.dev.common.BaseResponse;
import com.image.AiAccess.dev.model.DevChatRequest;
import com.image.AiAccess.dev.model.DevChatResponse;
import com.image.AiAccess.dev.model.SparkChatRequest;
import com.image.AiAccess.dev.model.SparkChatResponse;
import com.image.project.common.ErrorCode;
import com.image.project.exception.BusinessException;
import com.image.project.exception.ThrowUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author JXY
 * @version 1.0
 * @since 2024/6/5
 */
@Service
public class AiManager {

    @Resource
    private ImageYuCongMingClient client;

    @Resource
    private ImageSparkClient imageSparkClient;

    /**
     * AI 对话
     *
     * @param modelId 模型id
     * @param message 消息
     * @return 响应内容
     */
    public String doChat(long modelId, String message) {
        DevChatRequest devChatRequest = new DevChatRequest();
        devChatRequest.setModelId(modelId);
        devChatRequest.setMessage(message);

        BaseResponse<DevChatResponse> response = client.doChat(devChatRequest);
        ThrowUtils.throwIf(response == null, ErrorCode.SYSTEM_ERROR, "AI 响应错误");
        return response.getData().getContent();
    }

    /**
     * Spark 对话（用户优化genChart）
     *
     * @param userId  userId
     * @param message 问题
     */
    public String doChatSpark(@NotNull Long userId, String message) {
        SparkChatRequest sparkChatRequest = new SparkChatRequest();
        sparkChatRequest.setUserId(userId.toString());
        sparkChatRequest.setQuestion(message);
        try {
            BaseResponse<SparkChatResponse> response = imageSparkClient.doChatWithNoPrompt(sparkChatRequest);
            if (response == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 响应错误");
            }
            String content = response.getData().getContent();
            ThrowUtils.throwIf(content == null, ErrorCode.OPERATION_ERROR, "AI 响应错误");
            return content;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 响应错误");
        }
    }
}
