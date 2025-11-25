package com.zhao.zhaopicturebacked.task;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhao.zhaopicturebacked.config.RedissonClientConfig;
import com.zhao.zhaopicturebacked.cos.CosService;
import com.zhao.zhaopicturebacked.domain.Picture;
import com.zhao.zhaopicturebacked.enums.CodeEnum;
import com.zhao.zhaopicturebacked.service.PictureService;
import com.zhao.zhaopicturebacked.utils.ThrowUtil;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import java.util.stream.Collectors;

@Slf4j
public class cleanPictureJob implements Job {
    //线程池，用来执行异步删除任务的
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);



    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        log.info("现在的时间是：{}",new Date());
        log.info("执行删除图片的定时任务");
        //使用redis实现分布式锁 todo 这个锁还有问题，如果执行时间超过了锁的过期时间，就会出现问题
        // 分布式锁参数
        final String lockKey = "zhaopicture:task:clean_picture:lock";
        //final String lockValue = UUID.randomUUID().toString(); // 唯一值，防止误删
        //final long lockExpireSeconds = 30; // 锁初始过期时间（秒）
        //final long renewIntervalSeconds = 10; // 续期间隔（秒），小于过期时间
        //因为Quartz的对象的实例化是自己内部实现的，而@Resource等是用Spring容器创建的，Quartz无法感知到Spring容器里的Bean
        //所以我们得手动拿到容器中的对象
        PictureService pictureService = SpringContextHolder.getBean(PictureService.class);
        RedissonClient redissonClient = SpringContextHolder.getBean(RedissonClient.class);
        //使用redisson获取锁
        RLock lock = redissonClient.getLock(lockKey);
        //StringRedisTemplate stringRedisTemplate = SpringContextHolder.getBean(StringRedisTemplate.class);
        try {
            //Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue,lockExpireSeconds, TimeUnit.SECONDS);
            if(!lock.tryLock(0,-1,TimeUnit.SECONDS)){
                log.info("任务正在执行中");
                //定时任务只执行一次，所以如果没抢到锁，就可以直接走了
                return;
            }
            doSomeThing();
            log.info("线程{}抢到了锁，开始执行定时任务任务",Thread.currentThread());

        }catch (Exception e){
            log.error("图片删除定时任务出现异常", e);
            ThrowUtil.throwBusinessException(CodeEnum.SYSTEM_ERROR,"图片删除定时任务出现异常");
        }finally {
           //释放锁
            // stringRedisTemplate.delete(lockKey);
            if(lock.isHeldByCurrentThread()){
                log.info("线程{}释放锁",Thread.currentThread());
                lock.unlock();
            }
        }




//
//        try {
//
//            //2.将这些图片从COS对象存储中删除，并且删库
//            log.info("开始删除图片");
//            for(Picture picture:list){
//                String url = picture.getpUrl();
//                int index = url.indexOf("com/");
//                String key = url.substring(index + 4);
//                cosService.deletePicture( key);
//                boolean b = pictureService.removeById(picture.getId());
//                if(!b){
//                    log.info("数据库删除图片失败");
//                }
//
//            }
//
//            log.info("图片删除完成");
//            //todo 还没删除COS上的缩略图，以及可以考虑异步调用
//        }catch (Exception e){
//            log.error("图片删除失败");
//            ThrowUtil.throwBusinessException(CodeEnum.SYSTEM_ERROR,"图片删除失败");
//        }




    }

    private void doSomeThing( ) {
        //先查找所有创建时间和更新时间相同，且都是在365天前的图片
        //获取当前的日期和时间
        LocalDateTime now = LocalDateTime.now();
        //计算 360 天前的时间
        //minusDays() 方法会返回一个新的 LocalDateTime 对象，不会修改原对象
        LocalDateTime before360Days  = now.minus(1, ChronoUnit.DAYS);
        //3. 格式化输出为 yyyy-MM-dd HH:mm:ss（与数据库 datetime 兼容）
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedTime = before360Days.format(formatter);

        //构造查询条件
        QueryWrapper<Picture> pictureQueryWrapper = new QueryWrapper<>();
        pictureQueryWrapper.lt("create_time",formattedTime);

        PictureService pictureService = SpringContextHolder.getBean(PictureService.class);
        List<Picture> list = pictureService.list(pictureQueryWrapper);
        log.info("找到的图片有{}张",list.size());
        if(CollUtil.isEmpty( list)||list==null){
            log.info("没有找到符合条件的图片");
            return;
        }
        CosService cosService = SpringContextHolder.getBean(CosService.class);
        //使用线程池异步执行任务

        List<CompletableFuture<Void>> collect = list.stream().map(picture -> CompletableFuture.runAsync(() -> {
            try {
                Long count = pictureService.lambdaQuery().eq(Picture::getpUrl, picture.getpUrl()).count();
                if (count > 1){
                    log.info("图片{}被其他用户使用，不能删除",picture.getpUrl());
                    return;
                }
                //将这些图片从COS对象存储中删除，并且删库
                log.info("开始删除图片");


                String url = picture.getpUrl();
                int index = url.indexOf("com/");
                String key = url.substring(index + 4);
                cosService.deletePicture( key);
                boolean b = pictureService.removeById(picture.getId());
                if(!b){
                    log.info("数据库删除图片失败");
                }

                log.info("图片删除完成");
                //todo 还没删除COS上的缩略图，以及可以考虑异步调用
            }catch (Exception e){
                log.error("图片删除失败");
                ThrowUtil.throwBusinessException(CodeEnum.SYSTEM_ERROR,"图片删除失败");
            }

        }, executor)).collect(Collectors.toList());
        //collect里的一部任务完成了之后，voidCompletableFuture才完成，voidCompletableFuture统一了collect的完成情况
        CompletableFuture<Void> voidCompletableFuture = CompletableFuture.allOf(collect.toArray(new CompletableFuture[0]));

        //任务完成后的回调
        voidCompletableFuture.whenComplete((unused, throwable) -> {
            if (throwable != null) {
                log.error("异步删除任务出现异常", throwable);
            } else {
                log.info("所有异步删除任务完成");
            }
        });
        long time = ChronoUnit.SECONDS.between(now, LocalDateTime.now());
        log.info("执行时间为:{}",time);
    }


}
