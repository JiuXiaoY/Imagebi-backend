package com.image.project.bizmq;

import com.image.project.common.ErrorCode;
import com.image.project.constant.BiMqConstant;
import com.image.project.constant.CommonConstant;
import com.image.project.exception.BusinessException;
import com.image.project.manager.AiManager;
import com.image.project.model.entity.Chart;
import com.image.project.model.enums.GenChartStatusEnum;
import com.image.project.service.ChartService;
import com.image.project.websocket.WebSocketServer;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author JXY
 * @version 1.0
 * @since 2024/6/9
 */
@Component
@Slf4j
public class BiMessageConsumer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private ChartService chartService;

    @Resource
    private AiManager aiManager;

    @Resource
    private WebSocketServer webSocketServer;

    @SneakyThrows
    @RabbitListener(queues = BiMqConstant.BI_QUEUE_NAME, ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("receive message: {}", message);
        if (StringUtils.isBlank(message)) {
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息为空");
        }
        long chartId = Long.parseLong(message);
        Chart oldChart = chartService.getById(chartId);

        if (oldChart == null) {
            // 如果图表为空，拒绝消息并抛出业务异常
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图表为空");
        }

        // 将状态更改为执行中
        Chart updateChart = new Chart();
        updateChart.setId(oldChart.getId());
        updateChart.setExecStatus(GenChartStatusEnum.RUNNING.getStatus());
        boolean updateResult = chartService.updateById(updateChart);
        if (!updateResult) {
            // 如果更新图表执行中状态失败，拒绝消息并处理图表更新错误
            channel.basicNack(deliveryTag, false, false);
            chartService.handleChartUpdateError(oldChart.getId(), "更新图表执行中状态失败");
            return;
        }
        // 拿到用户输入
        String userInput = chartService.buildUserInput(oldChart);
        // 调用　AI 生成
        String result = aiManager.doChat(CommonConstant.BI_MODEL_ID, userInput);
        // 对返回结果进行分割
        String[] splits = result.split("【【【【【");
        // 校验
        if (splits.length < 3) {
            channel.basicNack(deliveryTag, false, false);
            chartService.handleChartUpdateError(oldChart.getId(), "AI 生成错误");
            return;
        }
        String genChart = splits[1].trim();
        String genResult = splits[2].trim();
        // 更新数据库
        Chart updateChartSuccess = new Chart();
        updateChartSuccess.setId(oldChart.getId());
        updateChartSuccess.setExecStatus(GenChartStatusEnum.SUCCESS.getStatus());
        updateChartSuccess.setGenChart(genChart);
        updateChartSuccess.setGenResult(genResult);
        boolean updateRes = chartService.updateById(updateChartSuccess);
        if (!updateRes) {
            // 如果更新图表成功状态失败，拒绝消息并处理图表更新错误
            channel.basicNack(deliveryTag, false, false);
            chartService.handleChartUpdateError(oldChart.getId(), "更新图表成功状态失败");
        }
        // 消息确认
        channel.basicAck(deliveryTag, false);
    }

    @SneakyThrows
    @RabbitListener(queues = BiMqConstant.BI_RETRY_QUEUE_NAME, ackMode = "MANUAL")
    public void receiveMessageWithUserId(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("receive message: {}", message);
        if (StringUtils.isBlank(message)) {
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息为空");
        }

        // 拿到 userId 和 ChartId
        String[] parts = message.split("=");
        if (parts.length != 2) {
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息格式错误");
        }

        long userId = Long.parseLong(parts[0]);
        long chartId = Long.parseLong(parts[1]);

        Chart oldChart = chartService.getById(chartId);

        if (oldChart == null) {
            // 如果图表为空，拒绝消息并抛出业务异常
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图表为空");
        }

        // 将状态更改为执行中
        Chart updateChart = new Chart();
        updateChart.setId(oldChart.getId());
        updateChart.setExecStatus(GenChartStatusEnum.RUNNING.getStatus());
        boolean updateResult = chartService.updateById(updateChart);
        if (!updateResult) {
            // 如果更新图表执行中状态失败，拒绝消息并处理图表更新错误
            channel.basicNack(deliveryTag, false, false);
            chartService.handleChartUpdateError(oldChart.getId(), "更新图表执行中状态失败");
            return;
        }
        // 拿到用户输入
        String userInput = chartService.buildUserInput(oldChart);
        // 调用　AI 生成
        String result = aiManager.doChat(CommonConstant.BI_MODEL_ID, userInput);
        // 对返回结果进行分割
        String[] splits = result.split("【【【【【");
        // 校验
        if (splits.length < 3) {
            channel.basicNack(deliveryTag, false, false);
            chartService.handleChartUpdateError(oldChart.getId(), "AI 生成错误");
            return;
        }
        // 图表结果
        String genChart = splits[1].trim();

        // 利用ai提高可靠性
        // genChart = new ValidEChartJsonFormatUtils().AiAmendResult(userId, MessageConstant.AMEND_MESSAGE + genChart);

        // 结论
        String genResult = splits[2].trim();
        // 更新数据库
        Chart updateChartSuccess = new Chart();
        updateChartSuccess.setId(oldChart.getId());
        updateChartSuccess.setExecStatus(GenChartStatusEnum.SUCCESS.getStatus());
        updateChartSuccess.setGenChart(genChart);
        updateChartSuccess.setGenResult(genResult);
        boolean updateRes = chartService.updateById(updateChartSuccess);
        if (!updateRes) {
            // 如果更新图表成功状态失败，拒绝消息并处理图表更新错误
            channel.basicNack(deliveryTag, false, false);
            chartService.handleChartUpdateError(oldChart.getId(), "更新图表成功状态失败");
            return;
        }
        // 消息确认
        channel.basicAck(deliveryTag, false);

        // todo 新增消息记录

        // 通知用户已经重新生成成功
        webSocketServer.sendMessage(String.valueOf(userId), "重新生成图表成功");
    }

    /**
     * 处理死信消息
     *
     * @param message     消息
     * @param channel     通道
     * @param deliveryTag 标识
     */
    @SneakyThrows
    @RabbitListener(queues = BiMqConstant.BI_DEAD_QUEUE_NAME, ackMode = "MANUAL")
    public void receiveDeadMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("receive message: {}", message);
        if (StringUtils.isBlank(message)) {
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息为空");
        }

        // 图表id
        long chartId;

        if (message.contains("=")) {
            // 拿到 userId 和 ChartId
            String[] parts = message.split("=");
            if (parts.length != 2) {
                channel.basicNack(deliveryTag, false, false);
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息格式错误");
            }
            // ChartId
            chartId = Long.parseLong(parts[1]);
        } else {
            chartId = Long.parseLong(message);
        }

        Chart oldChart = chartService.getById(chartId);

        if (oldChart == null) {
            // 如果图表为空，拒绝消息并抛出业务异常
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图表为空");
        }

        oldChart.setExecStatus(GenChartStatusEnum.FAILED.getStatus());
        oldChart.setExecMessage("AI 生成失败，请重试");
        boolean res = chartService.updateById(oldChart);

        if (!res) {
            // 如果更新图表成功状态失败，拒绝消息并处理图表更新错误
            channel.basicNack(deliveryTag, false, false);
            chartService.handleChartUpdateError(oldChart.getId(), "更新图表失败状态失败");
            return;
        }

        // 消息确认
        channel.basicAck(deliveryTag, false);
    }

}
