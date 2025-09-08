package com.hmdp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ApplicationShutdownListener implements ApplicationListener<ContextClosedEvent> {

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        log.info("应用正在关闭，开始清理资源...");
        
        try {
            // 获取Redis连接工厂并关闭
            RedisConnectionFactory connectionFactory = event.getApplicationContext()
                    .getBean(RedisConnectionFactory.class);
            
            if (connectionFactory != null) {
                log.info("正在关闭Redis连接工厂...");
                connectionFactory.getConnection().close();
                log.info("Redis连接工厂已关闭");
            }
        } catch (Exception e) {
            log.warn("关闭Redis连接时出现异常: {}", e.getMessage());
        }
        
        log.info("应用关闭完成");
    }
} 