package com.zhao.zhaopicturebacked.task;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;


public class cleanPictureRunner extends Thread{

    @Override
    public void run() {
        try {
            // 1、创建Scheduler（调度器）
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            Scheduler scheduler = schedulerFactory.getScheduler();

            //2.创建Trigger（触发器）
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("everyWeek", "picture") // 触发器名称和分组自定义
                    .withSchedule(CronScheduleBuilder.cronSchedule("0 0 0 1/7 * ?   ")) // 定时：此处cron表达式每周一天0点执行一次
                    .build();
            TriggerKey key = trigger.getKey();

            //3.创建Job（任务）
            JobDetail job = JobBuilder.newJob(cleanPictureJob.class)// 创建cleanPictureJob任务
                    .withIdentity(key.getName(), key.getGroup())// 任务名称和分组自定义
                    .build();
            //4、将Job和Trigger交给Scheduler调度
            scheduler.scheduleJob(job, trigger);
            // 5、启动Scheduler
            scheduler.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
