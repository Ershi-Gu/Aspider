package com.ershi.aspider.config;

import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * 线程池配置，该配置同时提供@Async以及本地事务表线程池配置
 *
 * @author Ershi
 * @date 2024/11/29
 */
@Configuration
@EnableAsync
public class ThreadPoolConfig implements AsyncConfigurer{

    /**
     * 项目通用虚拟线程池
     */
    public static final String ASPIDER_VIRTUAL_EXECUTOR = "aspiderVirtualExecutor";

    /**
     * 指定@Async使用的线程池
     *
     * @return {@link Executor}
     */
    @Override
    public Executor getAsyncExecutor() {
        return virtualExecutor();
    }

    /**
     * 项目通用通信虚拟线程池，该线程池并不作池化，由于虚拟线程属于非常轻量级的资源，因此，用时创建，用完就扔，不要池化虚拟线程。
     *
     * @return {@link Executor}
     */
    @Bean(ASPIDER_VIRTUAL_EXECUTOR)
    public Executor virtualExecutor() {
        // 虚拟线程的基础线程工厂
        ThreadFactory virtualFactory = Thread.ofVirtual()
                .name("Aspider-virtual-", 0)
                .factory();
        // 自定义线程工厂包装基础工厂，进行线程内的异常捕获
        ThreadFactory myFactory = new MyThreadFactory(virtualFactory);
        return Executors.newThreadPerTaskExecutor(myFactory);
    }
}
