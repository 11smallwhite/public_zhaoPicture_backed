package com.zhao.zhaopicturebacked.cache;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.zhao.zhaopicturebacked.model.PictureVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Component
@Slf4j
public class PicturePageCaffeineCache extends PicturePageCacheTemplate{

    @Resource
    private CaffeienCacheStrategy caffeienCacheStrategy ;

    @Override
    public Page<PictureVO> getCache(String key) {
        //查找Caffeine缓存
        String cache = caffeienCacheStrategy.getCache(key);
        if (cache != null){
            log.info("从Caffeine缓存里获取数据成功");
            Page<PictureVO> pictureVOPage = JSONUtil.toBean(cache, Page.class);
            return pictureVOPage;
        }
        return null;
    }

    @Override
    public void setCache(String key, String value) {
        caffeienCacheStrategy.setCache(key, value);
    }
}
