package com.image.project.websocket;

import com.image.project.common.ErrorCode;
import com.image.project.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author JXY
 * @version 1.0
 * @since 2024/6/13
 */
@Component
@Slf4j
@ServerEndpoint("/websocket/{userId}")
public class WebSocketServer {
    // 静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
    private static final AtomicInteger onlineCount = new AtomicInteger(0);

    // 存放所有在线的客户端
    private static final Map<String, Session> onlineSessionClientMap = new ConcurrentHashMap<>();

    // 连接的 userId 和 session
    private String userId;
    private Session session;

    /**
     * 连接建立成功时调用的方法
     *
     * @param session 唯一
     */
    @OnOpen
    public void onOpen(@PathParam("userId") String userId, Session session) {
        // 打印日志
        this.userId = userId;
        this.session = session;
        log.info("连接建立中...... session_Id = {} , userId = {}", session.getId(), userId);
        // 存储 , 连接数 +1
        onlineSessionClientMap.put(userId, session);
        onlineCount.incrementAndGet();
        log.info("连接建立成功...... session_Id = {} , userId = {}, size = {}", session.getId(), userId, onlineSessionClientMap.size());
    }

    /**
     * 连接关闭时执行的方法
     */
    @OnClose
    public void onClose(Session session) {
        if (session != null) {
            log.info("连接关闭中...... session_Id = {} , userId = {}", session.getId(), userId);
            // 移除 且 关闭
            try {
                session.close();
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "session 关闭失败");
            }
        }
        // 连接数 -1
        onlineSessionClientMap.remove(userId);
        onlineCount.decrementAndGet();
        log.info("连接关闭...... userId = {}", userId);
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        log.info("收到来自客户端的消息消息：{} , userId = {} , session_Id = {}", message, this.userId, session.getId());
        sendMessage("pong");
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("发生错误：{} , userId = {}", error.getMessage(), this.userId);
    }

    /**
     * 发送消息
     *
     * @param message 消息内容
     */
    public void sendMessage(String message) {
        try {
            this.session.getAsyncRemote().sendText(message);
        } catch (Exception e) {
            log.error("发送消息出错：{} , userId = {}", e.getMessage(), this.userId);
        }
    }

    public void sendMessage(String userId, String message) {
        try {
            Session sessionToSend = onlineSessionClientMap.get(userId);
            sessionToSend.getAsyncRemote().sendText(message);
        } catch (Exception e) {
            log.error("发送消息出错：{} , userId = {}", e.getMessage(), this.userId);
        }
    }
}
