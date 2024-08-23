package com.smart.sso.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /** Set the ThreadPoolExecutor's core pool size. */
    @Value("${async.corePoolSize:16}")
    private int corePoolSize;

    /** Set the ThreadPoolExecutor's maximum pool size. */
    @Value("${async.maxPoolSize:32}")
    private int maxPoolSize;

    /** Set the capacity for the ThreadPoolExecutor's BlockingQueue. */
    @Value("${async.queueCapacity:999}")
    private int queueCapacity;

    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new
                ThreadPoolTaskExecutor();
        /** . */
        executor.setCorePoolSize(corePoolSize);
        /** 在实际的应用中发现，这个参数并没有什么用 为什么说没用。当*/
        executor.setMaxPoolSize(maxPoolSize);
        /** 任务队列的最大值。超过此值会触发拒绝策略，默认会抛异常，该值的默认大小为integermax*/
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("AsyncExecutor-");
        executor.initialize();
        return executor;
    }
}

