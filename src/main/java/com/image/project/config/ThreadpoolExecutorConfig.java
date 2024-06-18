package com.image.project.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author JXY
 * @version 1.0
 * @since 2024/6/7
 */
@Configuration
public class ThreadpoolExecutorConfig {
    // 线程池参数配置，根据实际测试情况去配置
    // corePoolSize 核心线程数，正常情况下，系统应该能同时工作的线程数
    // maximumPoolSize 最大线程数，当线程数大于核心线程数时，多余的线程会进入队列，等待被执行
    // keepAliveTime 线程空闲时间，超过这个时间，线程会被销毁
    // unit 时间单位
    // workQueue 队列，当线程数大于核心线程数时，多余的线程会进入队列，等待被执行,存在一个长度
    // threadFactory 线程工厂，用于创建线程
    // handler 拒绝策略，当队列满了，且线程数大于最大线程数时，会执行拒绝策略

    /**
     * public ThreadPoolExecutor(int corePoolSize,
     * int maximumPoolSize,
     * long keepAliveTime,
     * TimeUnit unit,
     * BlockingQueue<Runnable> workQueue,
     * ThreadFactory threadFactory,
     * RejectedExecutionHandler handler)
     * corePoolSize = 2
     * 第一个任务进来时，会创建一个核心线程进行处理
     * 第二个任务进来时，由于线程数没有达到核心线程数，所以会在创建一个核心线程进行处理
     * workQueue = 4
     * 第三个任务进来，由于已经达到核心线程数了，所以会进入队列，等待被执行
     * ......
     * 第六个任务进来，由于队列没有满，依旧会放到队列进行等待，此时已经达到队列上限
     * maximumPoolSize = 4
     * 第七个任务进来，由于线程数没有达到最大线程数，所以会创建一个临时线程进行处理 (处理任务7)
     * 第八个任务进来，由于线程数没有达到最大线程数，所以会创建一个临时线程进行处理，(处理任务8) 此时已经达到最大线程数
     * 第九个任务进来，执行拒绝策略
     */
    private ThreadFactory threadFactory = new ThreadFactory() {
        private int count = 1;

        @Override
        public Thread newThread(@NotNull Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("bi_gen_chart_threadPool-" + count++);
            return thread;
        }
    };

    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(2,
                4,
                100,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(4),
                threadFactory);
        return threadPoolExecutor;
    }
}
