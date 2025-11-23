package com.zhao.zhaopicturebacked.cache;

import java.util.concurrent.TimeUnit;

public interface CacheStrategy {
    String getCache(String key);
    void setCache(String key, String value);
    void deleteCache(String key);

}
