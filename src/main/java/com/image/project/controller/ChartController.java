package com.image.project.controller;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.image.project.annotation.AuthCheck;
import com.image.project.bizmq.BiMessageProducer;
import com.image.project.common.BaseResponse;
import com.image.project.common.DeleteRequest;
import com.image.project.common.ErrorCode;
import com.image.project.common.ResultUtils;
import com.image.project.constant.CommonConstant;
import com.image.project.constant.MessageConstant;
import com.image.project.constant.UserConstant;
import com.image.project.exception.BusinessException;
import com.image.project.exception.ThrowUtils;
import com.image.project.manager.AiManager;
import com.image.project.manager.RedisLimiterManager;
import com.image.project.model.dto.chart.*;
import com.image.project.model.entity.Chart;
import com.image.project.model.entity.ChartMessage;
import com.image.project.model.entity.User;
import com.image.project.model.enums.GenChartStatusEnum;
import com.image.project.model.enums.MessageStatusEnum;
import com.image.project.model.enums.MessageTypeEnum;
import com.image.project.model.vo.BiResponse;
import com.image.project.service.ChartMessageService;
import com.image.project.service.ChartService;
import com.image.project.service.UserService;
import com.image.project.utils.ExcelUtils;
import com.image.project.utils.SqlUtils;
import com.image.project.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 图标信息接口
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private AiManager aiManager;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private BiMessageProducer biMessageProducer;

    @Resource
    private ChartMessageService chartMessageService;

    @Resource
    private WebSocketServer webSocketServer;

    // region 增删改查

    /**
     * 创建
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                     HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                       HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    // endregion

    /**
     * 编辑（用户）
     *
     * @param chartEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);
        User loginUser = userService.getLoginUser(request);
        long id = chartEditRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 获取查询包装类
     *
     * @param chartQueryRequest
     * @return
     */
    private QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chartQueryRequest.getId();
        String goal = chartQueryRequest.getGoal();
        String relatedName = chartQueryRequest.getRelatedName();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();

        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.like(StringUtils.isNotBlank(relatedName), "relatedName", relatedName);
        queryWrapper.eq(StringUtils.isNotBlank(chartType), "chartType", chartType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    // new content


    /**
     * 失败重试
     */
    @GetMapping("/gen/retry")
    public BaseResponse<BiResponse> genChartByAiRetry(Long chartId, HttpServletRequest request) {
        BiResponse biResponse = chartService.genChartByAiRetry(chartId, request);
        return ResultUtils.success(biResponse);
    }

    /**
     * 重试，通过消息队列
     * 其实就是重新发送消息
     */
    @GetMapping("/gen/retry/mq")
    public BaseResponse<BiResponse> genChartByAiMqRetry(Long chartId, HttpServletRequest request) {
        // 拿到 userId
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        // 发送消息给消息队列
        biMessageProducer.sendMessageWithUserId(userId + "=" + chartId);
        // 返回chartId
        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chartId);
        return ResultUtils.success(biResponse);
    }

    /**
     * 智能分析 （同步）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen")
    public BaseResponse<BiResponse> genChartByAi(@RequestPart("file") MultipartFile multipartFile,
                                                 GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        // 获取参数
        String relatedName = genChartByAiRequest.getRelatedName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();

        // 校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "分析目标不可以为空");
        ThrowUtils.throwIf(StringUtils.isBlank(relatedName), ErrorCode.PARAMS_ERROR, "请给本次分析起一个名字");
        ThrowUtils.throwIf(relatedName.length() > 100, ErrorCode.PARAMS_ERROR, "输入的名字过长");

        // 校验文件
        // 拿到用户文件的原始大小和文件名
        long fileSize = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();

        final long OME_MB = 1024 * 1024L;
        ThrowUtils.throwIf(fileSize > OME_MB, ErrorCode.PARAMS_ERROR, "文件超过 1M");

        // 取到文件名后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        // 定义合法的后缀列表
        final List<String> legalSuffixList = Arrays.asList("png", "jpg", "svg", "webp", "jpeg", "xlsx", "xls");
        ThrowUtils.throwIf(!legalSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件格式不支持");

        // 拿到登录得用户，必须登录了才能使用
        User loginUser = userService.getLoginUser(request);

        // 限流
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());

        // 构造用户输入
        StringBuilder userInput = new StringBuilder();
        // 拼接 goal
        userInput.append("分析需求: ").append('\n');

        userInput.append(goal).append('\n');
        if (StringUtils.isNotBlank(chartType)) {
            userInput.append(", 请使用").append(chartType).append("图表类型").append('\n');
        }

        // 读取到用户上传的文件，进行处理
        String excelToCsvResult = ExcelUtils.excelToCsv(multipartFile);
        // 拼接数据
        userInput.append("原始数据: ").append('\n');
        userInput.append(excelToCsvResult).append('\n');

        String result = aiManager.doChat(CommonConstant.BI_MODEL_ID, userInput.toString());

        // 对返回结果进行分割
        String[] splits = result.split("【【【【【");
        // 校验
        ThrowUtils.throwIf(splits.length < 3, ErrorCode.PARAMS_ERROR, "AI 生成错误");
        String genChart = splits[1].trim();
        String genResult = splits[2].trim();
        // 插入到数据库中
        Chart chart = new Chart();
        chart.setGoal(goal);
        chart.setChartData(excelToCsvResult);
        chart.setChartType(chartType);
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setUserId(loginUser.getId());
        chart.setRelatedName(relatedName);
        boolean saveResult = chartService.save(chart);

        ThrowUtils.throwIf(!saveResult, ErrorCode.PARAMS_ERROR, "保存失败");
        return ResultUtils.success(new BiResponse(genChart, genResult, chart.getId()));
    }

    /**
     * 智能分析 （异步）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/genByAsync")
    public BaseResponse<BiResponse> genChartByAiAsync(@RequestPart("file") MultipartFile multipartFile,
                                                      GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        // 获取参数
        String relatedName = genChartByAiRequest.getRelatedName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();

        // 校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "分析目标不可以为空");
        ThrowUtils.throwIf(StringUtils.isBlank(relatedName), ErrorCode.PARAMS_ERROR, "请给本次分析起一个名字");
        ThrowUtils.throwIf(relatedName.length() > 100, ErrorCode.PARAMS_ERROR, "输入的名字过长");

        // 校验文件
        // 拿到用户文件的原始大小和文件名
        long fileSize = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();

        final long OME_MB = 1024 * 1024L;
        ThrowUtils.throwIf(fileSize > OME_MB, ErrorCode.PARAMS_ERROR, "文件超过 1M");

        // 取到文件名后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        // 定义合法的后缀列表
        final List<String> legalSuffixList = Arrays.asList("png", "jpg", "svg", "webp", "jpeg", "xlsx", "xls");
        ThrowUtils.throwIf(!legalSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件格式不支持");

        // 拿到登录得用户，必须登录了才能使用
        User loginUser = userService.getLoginUser(request);

        // 限流
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());

        // 读取到用户上传的文件，进行处理
        String excelToCsvResult = ExcelUtils.excelToCsv(multipartFile);

        // 异步化，先保存到数据库中
        Chart oldChart = new Chart();
        oldChart.setGoal(goal);
        oldChart.setChartData(excelToCsvResult);
        oldChart.setChartType(chartType);
        oldChart.setUserId(loginUser.getId());
        oldChart.setRelatedName(relatedName);
        oldChart.setExecStatus(GenChartStatusEnum.WAITING.getStatus());
        boolean Result = chartService.save(oldChart);
        ThrowUtils.throwIf(!Result, ErrorCode.PARAMS_ERROR, "保存失败");

        // 获取用户输入
        String userInput = chartService.buildUserInput(oldChart);

        // 将生成图表的任务放入任务队列中
        CompletableFuture.runAsync(() -> {
            // 将状态更改为执行中
            Chart updateChart = new Chart();
            updateChart.setId(oldChart.getId());
            updateChart.setExecStatus(GenChartStatusEnum.RUNNING.getStatus());
            boolean updateResult = chartService.updateById(updateChart);
            if (!updateResult) {
                chartService.handleChartUpdateError(oldChart.getId(), "更新图表执行中状态失败");
                return;
            }
            // 调用　AI 生成
            String result = aiManager.doChat(CommonConstant.BI_MODEL_ID, userInput.toString());
            // 对返回结果进行分割
            String[] splits = result.split("【【【【【");
            // 校验
            if (splits.length < 3) {
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
                chartService.handleChartUpdateError(oldChart.getId(), "更新图表成功状态失败");
            }

            // 存储消息
            // 这是系统消息，
            ChartMessage chartMessage = new ChartMessage();
            chartMessage.setSenderId(UserConstant.SYSTEM_SENDER_ID);
            chartMessage.setReceiverId(loginUser.getId());
            chartMessage.setChartId(oldChart.getId());
            chartMessage.setMessageContent(MessageConstant.CHART_MESSAGE_SUCCESS);
            chartMessage.setMessageType(MessageTypeEnum.SYSTEM_MESSAGE.getStatus());
            chartMessage.setMessageStatus(MessageStatusEnum.UNREAD.getStatus());
            boolean saveRes = chartMessageService.save(chartMessage);
            ThrowUtils.throwIf(!saveRes, ErrorCode.PARAMS_ERROR, MessageConstant.CHART_MESSAGE_FAIL);
            // 通知
            // 获取用户 Id
            webSocketServer.sendMessage(String.valueOf(loginUser.getId()), MessageConstant.CHART_MESSAGE_SUCCESS);

        }, threadPoolExecutor);

        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(oldChart.getId());
        return ResultUtils.success(biResponse);
    }


    /**
     * 智能分析 （异步消息队列）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/genByAsync/mq")
    public BaseResponse<BiResponse> genChartByAiAsyncMq(@RequestPart("file") MultipartFile multipartFile,
                                                        GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        // 获取参数
        String relatedName = genChartByAiRequest.getRelatedName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();

        // 校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "分析目标不可以为空");
        ThrowUtils.throwIf(StringUtils.isBlank(relatedName), ErrorCode.PARAMS_ERROR, "请给本次分析起一个名字");
        ThrowUtils.throwIf(relatedName.length() > 100, ErrorCode.PARAMS_ERROR, "输入的名字过长");

        // 校验文件
        // 拿到用户文件的原始大小和文件名
        long fileSize = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();

        final long OME_MB = 1024 * 1024L;
        ThrowUtils.throwIf(fileSize > OME_MB, ErrorCode.PARAMS_ERROR, "文件超过 1M");

        // 取到文件名后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        // 定义合法的后缀列表
        final List<String> legalSuffixList = Arrays.asList("png", "jpg", "svg", "webp", "jpeg", "xlsx", "xls");
        ThrowUtils.throwIf(!legalSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件格式不支持");

        // 拿到登录得用户，必须登录了才能使用
        User loginUser = userService.getLoginUser(request);

        // 限流
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());

        // 读取到用户上传的文件，进行处理
        String excelToCsvResult = ExcelUtils.excelToCsv(multipartFile);

        // 异步化，先保存到数据库中
        Chart oldChart = new Chart();
        oldChart.setGoal(goal);
        oldChart.setChartData(excelToCsvResult);
        oldChart.setChartType(chartType);
        oldChart.setUserId(loginUser.getId());
        oldChart.setRelatedName(relatedName);
        oldChart.setExecStatus(GenChartStatusEnum.WAITING.getStatus());
        boolean Result = chartService.save(oldChart);
        ThrowUtils.throwIf(!Result, ErrorCode.PARAMS_ERROR, "保存失败");

        Long newChartId = oldChart.getId();
        // 提交一个任务
        // todo 任务队列满了之后，抛异常的情况
        // 原始提交任务（无通知）
        // biMessageProducer.sendMessage(String.valueOf(newChartId));
        // 提交一个任务（有通知）
        biMessageProducer.sendMessageWithUserId(loginUser.getId() + "=" + newChartId);

        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(newChartId);
        return ResultUtils.success(biResponse);
    }

}
