package com.zhao.zhaopicturebacked.cache;

import cn.hutool.json.JSONUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RedisCacheStrategy implements CacheStrategy{

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public String getCache(String key) {
        //redis查找缓存
        ValueOperations<String, String> stringStringValueOperations = stringRedisTemplate.opsForValue();
        String cache = stringStringValueOperations.get(key);
        return cache;
    }


    public void setCache(String key, String value, long timeOut, TimeUnit timeUnit) {
        //给redis设置缓存,并设置缓存过期时间
        log.info("将数据缓存进redis");
        stringRedisTemplate.opsForValue().set(key, value, timeOut,timeUnit);
    }
    @Override
    public void setCache(String key, String value) {
        //给redis设置缓存,并设置缓存过期时间
        log.info("将数据缓存进redis");
        stringRedisTemplate.opsForValue().set(key, value, 60*60, TimeUnit.SECONDS);
    }

    @Override
    public void deleteCache(String key) {
        stringRedisTemplate.delete(key);
    }
}
