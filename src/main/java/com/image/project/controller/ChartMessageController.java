package com.image.project.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.image.project.common.BaseResponse;
import com.image.project.common.DeleteRequest;
import com.image.project.common.ErrorCode;
import com.image.project.common.ResultUtils;
import com.image.project.exception.BusinessException;
import com.image.project.exception.ThrowUtils;
import com.image.project.model.dto.chartmessage.ChartMessageQueryRequest;
import com.image.project.model.dto.chartmessage.ChartMessageQueryResponse;
import com.image.project.model.entity.Chart;
import com.image.project.model.entity.ChartMessage;
import com.image.project.model.entity.User;
import com.image.project.model.enums.GenChartStatusEnum;
import com.image.project.model.enums.MessageStatusEnum;
import com.image.project.service.ChartMessageService;
import com.image.project.service.ChartService;
import com.image.project.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author JXY
 * @version 1.0
 * @since 2024/6/14
 */

@RestController
@RequestMapping("/chartMsg")
@Slf4j
public class ChartMessageController {
    /**
     * 根据 id 获取当前所有 receiverId = id 的消息
     */

    @Resource
    private ChartMessageService chartMessageService;

    @Resource
    private UserService userService;

    @Resource
    private ChartService chartService;

    @GetMapping("/get")
    public BaseResponse<List<ChartMessageQueryResponse>> getAllChartMsg(ChartMessageQueryRequest chartMessageQueryRequest, HttpServletRequest request) {
        Long receiverId = chartMessageQueryRequest.getReceiverId();
        if (receiverId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<ChartMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("receiverId", receiverId);
        List<ChartMessage> list = chartMessageService.list(queryWrapper);
        List<ChartMessageQueryResponse> responseList = list.stream()
                .map(this::convertAs)
                .collect(Collectors.toList());
        return ResultUtils.success(responseList);
    }

    /**
     * 标记已读
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/tickleToRead")
    public BaseResponse<Boolean> tickleToRead(Long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "id不存在");
        }
        ChartMessage chartMessage = chartMessageService.getById(id);
        ThrowUtils.throwIf(chartMessage == null, ErrorCode.PARAMS_ERROR, "id不存在");
        chartMessage.setMessageStatus(MessageStatusEnum.READ.getStatus());
        boolean result = chartMessageService.updateById(chartMessage);
        return ResultUtils.success(result);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChartMessage(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        ChartMessage chartMessage = chartMessageService.getById(id);
        ThrowUtils.throwIf(chartMessage == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!chartMessage.getReceiverId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartMessageService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 获取当前未读消息的数量
     *
     * @param chartMessageQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/UnreadCount")
    public BaseResponse<Integer> UnreadCount(@RequestBody ChartMessageQueryRequest chartMessageQueryRequest, HttpServletRequest request) {
        if (chartMessageQueryRequest == null || chartMessageQueryRequest.getReceiverId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        Long receiverId = chartMessageQueryRequest.getReceiverId();
        // 仅本人或管理员可查询
        if (!receiverId.equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        QueryWrapper<ChartMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper
                .eq("receiverId", receiverId)
                .eq("messageStatus", MessageStatusEnum.UNREAD.getStatus());
        long count = chartMessageService.count(queryWrapper);
        return ResultUtils.success((int) count);
    }


    /**
     * 长轮询 (本来是打算封装成异步任务，但是发现没有意义，因为前端已经使用了轮询，所以就直接用轮询了)
     */
    private static final Map<String, DeferredResult<BaseResponse<Integer>>> userRequests = new ConcurrentHashMap<>();

    @GetMapping("/long-polling")
    public DeferredResult<BaseResponse<Integer>> getUnreadMessages(@RequestParam String userId) {
        DeferredResult<BaseResponse<Integer>> deferredResult = new DeferredResult<>(30000L);

        userRequests.put(userId, deferredResult);

        deferredResult.onTimeout(() -> {
            deferredResult.setResult(ResultUtils.success(0));
            userRequests.remove(userId);
        });
        deferredResult.onCompletion(() -> userRequests.remove(userId));
        return deferredResult;
    }


    public ChartMessageQueryResponse convertAs(ChartMessage chartMessage) {
        ChartMessageQueryResponse chartMessageQueryResponse = new ChartMessageQueryResponse();
        chartMessageQueryResponse.setId(chartMessage.getId());
        chartMessageQueryResponse.setMessageType(chartMessage.getMessageType());

        Long senderId = chartMessage.getSenderId();
        User sender = userService.getById(senderId);

        Long chartId = chartMessage.getChartId();
        Chart chart = chartService.getById(chartId);

        chartMessageQueryResponse.setAvatarUrl(sender.getUserAvatar());
        chartMessageQueryResponse.setRelatedName(chart.getRelatedName());
        String messageContent;
        if (chart.getExecStatus() == GenChartStatusEnum.SUCCESS.getStatus()) {
            messageContent = "图表已经生成，前往我的图表页查看";
        } else {
            messageContent = "图表生成失败，请重试";
        }
        chartMessageQueryResponse.setMessageContent(messageContent);
        chartMessageQueryResponse.setMessageStatus(chartMessage.getMessageStatus());
        chartMessageQueryResponse.setMessageTime(chartMessage.getMessageTime());

        return chartMessageQueryResponse;
    }
}
