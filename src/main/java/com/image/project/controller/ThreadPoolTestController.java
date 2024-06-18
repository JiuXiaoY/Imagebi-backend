package com.image.project.controller;

import cn.hutool.json.JSONUtil;
import com.image.AiAccess.dev.client.ImageQwenMaxClient;
import com.image.AiAccess.dev.client.ImageSparkClient;
import com.image.AiAccess.dev.common.BaseResponse;
import com.image.AiAccess.dev.model.QwenChartRequest;
import com.image.AiAccess.dev.model.QwenChatResponse;
import com.image.AiAccess.dev.model.SparkChatRequest;
import com.image.AiAccess.dev.model.SparkChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author JXY
 * @version 1.0
 * @since 2024/6/7
 */

@RestController
@Slf4j
@Profile({"dev", "local"})
public class ThreadPoolTestController {

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private ImageSparkClient imageSparkClient;

    @Resource
    private ImageQwenMaxClient imageQwenMaxClient;

    @GetMapping("/hello")
    public String hello() {
        try {
            SparkChatRequest sparkChatRequest = new SparkChatRequest();
            sparkChatRequest.setUserId("123");
            sparkChatRequest.setQuestion("给一个java入门程序");
            BaseResponse<SparkChatResponse> result = imageSparkClient.doChat(sparkChatRequest);
            System.out.println(result.getData().getContent());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return "hello";
    }

    @GetMapping("/hi")
    public String hi() {
        try {
            QwenChartRequest qwenChartRequest = new QwenChartRequest();
            qwenChartRequest.setQuestion("分析需求：\n" +
                    "分析网站用户的增长情况\n" +
                    "原始数据：\n" +
                    "日期,用户数\n" +
                    "1号,10\n" +
                    "2号,40\n" +
                    "3号,80");
            BaseResponse<QwenChatResponse> chat = imageQwenMaxClient.doChat(qwenChartRequest);
            System.out.println(chat.getData().getContent());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return "hello";
    }

    @GetMapping("/add")
    public void work(String name) {
        CompletableFuture.runAsync(() -> {
            log.info("work: " + name + ", work man" + Thread.currentThread().getName());
            try {
                Thread.sleep(600000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, threadPoolExecutor);
    }

    @GetMapping("/get")
    public String threadPoolMsg() {
        Map<String, Object> Msg = new HashMap<>();
        int size = threadPoolExecutor.getQueue().size();
        Msg.put("队列长度", size);
        long taskCount = threadPoolExecutor.getTaskCount();
        Msg.put("任务总数", taskCount);
        long completedTaskCount = threadPoolExecutor.getCompletedTaskCount();
        Msg.put("已完成任务数", completedTaskCount);
        int activeCount = threadPoolExecutor.getActiveCount();
        Msg.put("活跃线程数", activeCount);
        return JSONUtil.toJsonStr(Msg);
    }

}
