package com.zhao.zhaopicturebacked.cache;


import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhao.zhaopicturebacked.domain.Picture;
import com.zhao.zhaopicturebacked.domain.User;
import com.zhao.zhaopicturebacked.model.PictureVO;
import com.zhao.zhaopicturebacked.request.picture.PictureQueryRequest;
import com.zhao.zhaopicturebacked.service.PictureService;
import com.zhao.zhaopicturebacked.service.UserService;
import com.zhao.zhaopicturebacked.service.impl.PictureServiceImpl;
import com.zhao.zhaopicturebacked.utils.ResultUtil;
import com.zhao.zhaopicturebacked.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
@Slf4j
@Component
public abstract class PicturePageCacheTemplate {
    @Resource
    private PictureService pictureService;
    @Resource
    private UserService userService;
    @Resource
    private PictureServiceImpl pictureServiceImpl;



    public Page<PictureVO> getPicturePageCache(PictureQueryRequest pictureQueryRequest) {

        //先构造缓存key
        String jsonStr = JSONUtil.toJsonStr(pictureQueryRequest);
        String ma5HexJsonStr = DigestUtils.md5DigestAsHex(jsonStr.getBytes());
        String key = "picture:pagePictureVOCache"+ma5HexJsonStr;

        Page<PictureVO> cache = getCache(key);
        if (cache != null){
            log.info("从缓存里获取数据成功");
            return cache;
        }

        Page<Picture> picturePage = pictureService.selectPage(pictureQueryRequest);
        //如果没查到数据，就直接返回空列表,同时也将空列表缓存进redis和Caffeine，防止用户恶意访问不存在的数据,使得数据库压力变大
        List<Picture> pictureList = picturePage.getRecords();
        if(pictureList.size()==0 ){
            setCache(key,JSONUtil.toJsonStr(new Page<PictureVO>()));
            return new Page<PictureVO>();
        }
        List<PictureVO> pictureVOList = pictureList.stream().map(picture ->pictureServiceImpl.getPictureVOByPicture(picture)).collect(Collectors.toList());
        //优化点，PictureVO里的UserVO字段需要回库查询数据，很多图片的userId可能都是一样的，一样的userId我们没必要查询多遍
        //先将UserId进行去重
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userService.listByIds(userIdSet).stream().collect(Collectors.toMap(User::getId, user -> user));
        pictureVOList.forEach(pictureVO -> {
            if (userMap.containsKey(pictureVO.getUserId())){
                pictureVO.setUserVO(UserUtil.getUserVOByUser(userMap.get(pictureVO.getUserId())));
            }
        });
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        pictureVOPage.setRecords(pictureVOList);

        setCache(key,JSONUtil.toJsonStr(pictureVOPage));
        return pictureVOPage;

    }

    public abstract Page<PictureVO> getCache(String key);
    public abstract void setCache(String key, String value);
}
