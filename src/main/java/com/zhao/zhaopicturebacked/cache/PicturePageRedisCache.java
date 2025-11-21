package com.zhao.zhaopicturebacked.cache;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhao.zhaopicturebacked.model.PictureVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class PicturePageRedisCache extends PicturePageCacheTemplate{

    @Resource
    private RedisCacheStrategy redisCacheStrategy; ;
    @Override
    public Page<PictureVO> getCache(String key) {
        String cache = redisCacheStrategy.getCache(key);
        if(cache != null){
            log.info("从redis里获取数据成功");
            Page<PictureVO> pictureVOPage = JSONUtil.toBean(cache, Page.class);
            return pictureVOPage;
        }
        return null;
    }

    @Override
    public void setCache(String key, String value) {

        redisCacheStrategy.setCache(key, value);

    }
}
