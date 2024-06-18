package com.image.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.image.project.common.ErrorCode;
import com.image.project.common.ResultUtils;
import com.image.project.constant.CommonConstant;
import com.image.project.constant.MessageConstant;
import com.image.project.constant.UserConstant;
import com.image.project.exception.BusinessException;
import com.image.project.exception.ThrowUtils;
import com.image.project.manager.AiManager;
import com.image.project.mapper.ChartMapper;
import com.image.project.model.entity.Chart;
import com.image.project.model.entity.ChartMessage;
import com.image.project.model.entity.User;
import com.image.project.model.enums.GenChartStatusEnum;
import com.image.project.model.enums.MessageStatusEnum;
import com.image.project.model.enums.MessageTypeEnum;
import com.image.project.model.vo.BiResponse;
import com.image.project.service.ChartService;
import com.image.project.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 *
 */
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
        implements ChartService {

    @Resource
    private UserService userService;

    @Resource
    private AiManager aiManager;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    public void handleChartUpdateError(Long chartId, String execMessage) {
        Chart updateChart = new Chart();
        updateChart.setId(chartId);
        updateChart.setExecMessage(execMessage);
        updateChart.setExecStatus(GenChartStatusEnum.FAILED.getStatus());
        boolean updateRes = this.updateById(updateChart);
        if (!updateRes) {
            log.error("更新图表失败状态失败" + chartId + ',' + execMessage);
        }
    }

    @Override
    public String buildUserInput(Chart chart) {
        // 获取分析目标
        String goal = chart.getGoal();
        // 获取需要生成图表类型
        String chartType = chart.getChartType();
        // 获取原始数据
        String excelToCsvResult = chart.getChartData();
        // 构造用户输入
        StringBuilder userInput = new StringBuilder();
        // 拼接 goal
        userInput.append("分析需求: ").append('\n');

        userInput.append(goal).append('\n');
        if (StringUtils.isNotBlank(chartType)) {
            // 拼接图标类型，不为空的话
            userInput.append(", 请使用").append(chartType).append("图表类型").append('\n');
        }

        // 拼接数据
        userInput.append("原始数据: ").append('\n');
        userInput.append(excelToCsvResult).append('\n');
        return userInput.toString();
    }

    /**
     * 重试 （基于线程池）
     *
     * @param chartId 图表Id
     * @param request 请求
     */
    @Override
    public BiResponse genChartByAiRetry(Long chartId, HttpServletRequest request) {
        // 校验
        if (chartId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图表Id不能小于0");
        }
        // 获取图表对象
        Chart chart = this.getById(chartId);
        ThrowUtils.throwIf(chart == null, ErrorCode.NOT_FOUND_ERROR, "图表不存在");
        // 校验权限，只有管理员和本人可以重试
        User loginUser = userService.getLoginUser(request);
        if (!chart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 执行相关逻辑
        // 将生成图表的任务放入任务队列中
        CompletableFuture.runAsync(() -> {
            // 将状态更改为执行中
            chart.setId(chart.getId());
            chart.setExecStatus(GenChartStatusEnum.RUNNING.getStatus());
            boolean updateResult = this.updateById(chart);
            if (!updateResult) {
                this.handleChartUpdateError(chart.getId(), "更新图表执行中状态失败");
                return;
            }
            // 调用　AI 生成
            String result = aiManager.doChat(CommonConstant.BI_MODEL_ID, this.buildUserInput(chart));
            // 对返回结果进行分割
            String[] splits = result.split("【【【【【");
            // 校验
            if (splits.length < 3) {
                this.handleChartUpdateError(chart.getId(), "AI 生成错误");
                return;
            }
            String genChart = splits[1].trim();
            String genResult = splits[2].trim();
            // 更新数据库
            Chart updateChartSuccess = new Chart();
            updateChartSuccess.setId(chart.getId());
            updateChartSuccess.setExecStatus(GenChartStatusEnum.SUCCESS.getStatus());
            updateChartSuccess.setGenChart(genChart);
            updateChartSuccess.setGenResult(genResult);
            boolean updateRes = this.updateById(updateChartSuccess);
            if (!updateRes) {
                this.handleChartUpdateError(chart.getId(), "更新图表成功状态失败");
            }

            // 存储消息
            // 这是系统消息，
            // todo 发送消息

        }, threadPoolExecutor);

        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chartId);
        return biResponse;
    }
}




