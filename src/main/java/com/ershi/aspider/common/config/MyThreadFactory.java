package com.ershi.aspider.common.config;

import com.ershi.aspider.common.exception.GlobalThreadUncaughtExceptionHandler;
import lombok.AllArgsConstructor;

import java.util.concurrent.ThreadFactory;

/**
 * 自定义线程工厂
 * @author Ershi
 * @date 2024/11/30
 */
@AllArgsConstructor
public class MyThreadFactory implements ThreadFactory {

    /**
     * 基础线程工厂
     */
    private ThreadFactory baseThreadFactory;

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = baseThreadFactory.newThread(r);
        // 执行自定义线程工厂内容
        thread.setUncaughtExceptionHandler(GlobalThreadUncaughtExceptionHandler.getInstance());
        return thread;
    }
}
