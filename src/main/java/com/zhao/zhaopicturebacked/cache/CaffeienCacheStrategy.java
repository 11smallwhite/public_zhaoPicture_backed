package com.zhao.zhaopicturebacked.cache;

import cn.hutool.json.JSONUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CaffeienCacheStrategy implements CacheStrategy{
    private final Cache<String, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(1024)
            .maximumSize(10000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    @Override
    public String getCache(String key) {
        String caffeineCache = LOCAL_CACHE.getIfPresent(key);
        return caffeineCache;
    }


    @Override
    public void setCache(String key, String value) {
        log.info("将数据写入Caffeine缓存");
        LOCAL_CACHE.put(key, value);
    }

    @Override
    public void deleteCache(String key) {
        LOCAL_CACHE.invalidate(key);
    }
}
