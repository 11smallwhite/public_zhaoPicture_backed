package com.zhao.zhaopicturebacked.cache;

public interface CacheStrategy {
    String getCache(String key);
    void setCache(String key, String value);}
