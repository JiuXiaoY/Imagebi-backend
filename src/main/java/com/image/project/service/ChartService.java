package com.image.project.service;

import com.image.project.model.entity.Chart;
import com.baomidou.mybatisplus.extension.service.IService;
import com.image.project.model.vo.BiResponse;

import javax.servlet.http.HttpServletRequest;

/**
 *
 */
public interface ChartService extends IService<Chart> {
    /**
     * 处理更新异常，并记录对应数据中
     *
     * @param chartId     图标id
     * @param execMessage 执行信息
     */
    void handleChartUpdateError(Long chartId, String execMessage);

    /**
     * 构造用户输入
     *
     * @param chart 图标
     * @return 拼接后的信息
     */
    String buildUserInput(Chart chart);

    /**
     * 重试 (线程池)
     * @param chartId 图表Id
     * @param request 请求
     */
    BiResponse genChartByAiRetry(Long chartId, HttpServletRequest request);
}
