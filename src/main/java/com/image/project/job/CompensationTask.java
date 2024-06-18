package com.image.project.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.image.project.bizmq.BiMessageProducer;
import com.image.project.model.entity.Chart;
import com.image.project.model.enums.GenChartStatusEnum;
import com.image.project.service.ChartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;

/**
 * @author JXY
 * @version 1.0
 * @since 2024/6/18
 */
// todo 取消注释及开启任务
// @Component
@Slf4j
public class CompensationTask {

    @Resource
    private ChartService chartService;

    @Resource
    private BiMessageProducer biMessageProducer;

    /**
     * 每10分钟执行一次，拉取数据库中执行失败的任务，发给消息队列重新执行
     */
    @Scheduled(fixedRate = 600 * 1000)
    public void run() {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("execStatus", GenChartStatusEnum.FAILED.getStatus());
        Chart failChart = chartService.getOne(queryWrapper);

        if (failChart == null) {
            log.info("没有需要补偿的任务");
            return;
        }

        // 拿到 chartId
        Long failChartId = failChart.getId();
        log.info("开始执行补偿任务 chartId = {}", failChartId);
        // 给队列发消息
        biMessageProducer.sendMessageWithUserId(String.valueOf(failChartId));
    }
}
