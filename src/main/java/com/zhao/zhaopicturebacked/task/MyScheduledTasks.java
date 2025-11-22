package com.zhao.zhaopicturebacked.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * SpringBoot的定时任务实现方法
 */
@Profile("dev")//在你的 @Component 类上添加 @Profile 注解，并指定一个或多个环境名称。只有当应用启动时激活了这些环境，该 Bean 才会被创建。
@Component
public class MyScheduledTasks {

    private static final Logger log = LoggerFactory.getLogger(MyScheduledTasks.class);

    /**
     * 每5秒执行一次。
     */
    @Scheduled(fixedRate = 5000)
    public void reportCurrentTime() {
        log.info("当前时间：{}，执行了 fixedRate 任务", LocalDateTime.now());
    }

    /**
     * 上一次任务执行完毕后，间隔5秒再执行。
     */
    @Scheduled(fixedDelay = 5000)
    public void doTaskWithFixedDelay() {
        log.info("当前时间：{}，开始执行 fixedDelay 任务", LocalDateTime.now());
        try {
            // 模拟一个耗时操作
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("当前时间：{}，完成了 fixedDelay 任务", LocalDateTime.now());
    }

    /**
     * 使用 Cron 表达式定义执行时间。
     * 例如：每天早上 8 点执行。
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void doTaskAt8AM() {
        log.info("早上好！当前时间：{}，执行了 cron 任务", LocalDateTime.now());
    }
}