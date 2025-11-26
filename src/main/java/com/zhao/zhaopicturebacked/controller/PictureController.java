package com.zhao.zhaopicturebacked.controller;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.zhao.zhaopicturebacked.annotation.AuthType;
import com.zhao.zhaopicturebacked.cache.*;
import com.zhao.zhaopicturebacked.common.BaseResponse;
import com.zhao.zhaopicturebacked.common.UserConstant;
import com.zhao.zhaopicturebacked.domain.Picture;
import com.zhao.zhaopicturebacked.domain.User;
import com.zhao.zhaopicturebacked.enums.AuditStatusEnum;
import com.zhao.zhaopicturebacked.enums.CodeEnum;
import com.zhao.zhaopicturebacked.model.LoginUserVO;
import com.zhao.zhaopicturebacked.model.PictureTagCategory;
import com.zhao.zhaopicturebacked.model.PictureVO;
import com.zhao.zhaopicturebacked.model.UserVO;
import com.zhao.zhaopicturebacked.request.DeleteRequest;
import com.zhao.zhaopicturebacked.request.picture.*;
import com.zhao.zhaopicturebacked.service.PictureService;
import com.zhao.zhaopicturebacked.service.UserService;
import com.zhao.zhaopicturebacked.service.impl.PictureServiceImpl;
import com.zhao.zhaopicturebacked.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/picture")
@Slf4j
public class PictureController {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;
    @Resource
    private RedisCacheStrategy redisCacheStrategy;
    @Resource
    private CaffeienCacheStrategy caffeienCacheStrategy;


    @Autowired
    private PictureServiceImpl pictureServiceImpl;


    @Resource
    private RedissonClient redissonClient;




    /**
     * 通过文件上传图片
     * @param multipartFile
     * @param request
     * @return
     */
    @PostMapping("/upload")
    @AuthType(userType = UserConstant.USER)
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest,HttpServletRequest request) {
        UserVO loginUserVO = TokenUtil.getLoginUserVOFromCookie(request);

        Long userId = loginUserVO.getId();

        PictureVO pictureVO = pictureService.uploadPicture(multipartFile,pictureUploadRequest, loginUserVO);


        User user = userService.getById(userId);
        UserVO userVO = UserUtil.getUserVOByUser(user);
        pictureVO.setUserVO(userVO);

       return ResultUtil.success(pictureVO);
    }

    /**
     * 通过url上传图片
     * @param
     * @param request
     * @return
     */
    @PostMapping("/upload/url")
    @AuthType(userType = UserConstant.USER)
    public BaseResponse<PictureVO> uploadPictureByUrl(PictureUploadRequest pictureUploadRequest,HttpServletRequest request) {
        UserVO loginUserVO = TokenUtil.getLoginUserVOFromCookie(request);

        Long userId = loginUserVO.getId();

        String fileUrl = pictureUploadRequest.getFileUrl();

        PictureVO pictureVO = pictureService.uploadPicture(fileUrl,pictureUploadRequest, loginUserVO);


        User user = userService.getById(userId);
        UserVO userVO = UserUtil.getUserVOByUser(user);
        pictureVO.setUserVO(userVO);
        return ResultUtil.success(pictureVO);
    }

    @PostMapping("/upload/batch")
    @AuthType(userType = UserConstant.ADMIN)
    public BaseResponse<Integer> uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, HttpServletRequest request, HttpServletResponse response) {

        UserVO loginUserVO = TokenUtil.getLoginUserVOFromCookie(request);

        int count = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUserVO);
        return ResultUtil.success(count,"上传成功");
    }



    /**
     * 删除图片
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @AuthType(userType = 0)
    public BaseResponse<Long> deletePictureById(@RequestBody DeleteRequest deleteRequest,HttpServletRequest request){
        UserVO loginUserVO = TokenUtil.getLoginUserVOFromCookie(request);
        Long delete = pictureService.deletePicture(deleteRequest,loginUserVO);
        return ResultUtil.success(delete,"删除成功");
    }


    /**
     * 分页查询图片（用户可用）
     * @param pictureQueryRequest
     * @return
     */
    @PostMapping("/page/select/query")
    //todo 暂时优化到这了
    public BaseResponse<Page<PictureVO>> select(@RequestBody PictureQueryRequest pictureQueryRequest,HttpServletRequest request){

        String servletPath = request.getServletPath();
        if(!servletPath.contains("admin")){
            pictureQueryRequest.setAuditStatus(AuditStatusEnum.REVIEW_PASS.getCode());
        }
        //根据PictureQueryRequest使用MD5加密构造缓存key
        String key = getCacheKey(pictureQueryRequest);
        Page<PictureVO> pictureVOPage = null;
        //查找Caffeine缓存
//        String cache = caffeienCacheStrategy.getCache(key);
//        if (cache != null){
//            log.info("从Caffeine缓存里获取数据成功");
//            pictureVOPage = JSONUtil.toBean(cache,new TypeReference<Page<PictureVO>>() {}, true);
//            return ResultUtil.success(pictureVOPage);
//        }
//        //redis查找缓存
//        cache = redisCacheStrategy.getCache(key);
//        if(cache != null){
//            log.info("从redis里获取数据成功");
//            pictureVOPage = JSONUtil.toBean(cache, new TypeReference<Page<PictureVO>>() {}, true);
//            //将缓存写入Caffeine缓存
//            caffeienCacheStrategy.setCache(key,cache);
//            return ResultUtil.success(pictureVOPage);
//        }

        //锁可以再细一点，查询不同的分页用的锁也不同
        String jsonStr = JSONUtil.toJsonStr(pictureQueryRequest);
        String lockKeyMd5 = DigestUtils.md5DigestAsHex(jsonStr.getBytes(StandardCharsets.UTF_8));
        String lockkey = String.format("zhaopicture:controller:select_picture:%s:lock",lockKeyMd5);

        RLock lock = redissonClient.getLock(lockkey);
        try{
            //如果获取锁失败则返回
            if(!lock.tryLock(10,-1,TimeUnit.SECONDS)){
                log.info("获取锁失败");
                return ResultUtil.success(pictureVOPage);
            }

//            //抢到了锁之后，再次查询缓存
//            cache = caffeienCacheStrategy.getCache(key);
//            if (cache != null){
//                log.info("从Caffeine缓存里获取数据成功");
//                pictureVOPage = JSONUtil.toBean(cache, new TypeReference<Page<PictureVO>>() {}, true);
//                return ResultUtil.success(pictureVOPage);
//            }
//            cache = redisCacheStrategy.getCache(key);
//            if(cache != null){
//                log.info("从redis里获取数据成功");
//                pictureVOPage = JSONUtil.toBean(cache, new TypeReference<Page<PictureVO>>() {}, true);
//                //将缓存写入Caffeine缓存
//                caffeienCacheStrategy.setCache(key,cache);
//                return ResultUtil.success(pictureVOPage);
//            }
            //如果还是没有缓存，就需要查数据库了
            Page<Picture> picturePage = pictureService.selectPage(pictureQueryRequest);
            //如果没查到数据，就直接返回空列表,同时也将空列表缓存进redis和Caffeine，防止用户恶意访问不存在的数据,使得数据库压力变大
            List<Picture> pictureList = picturePage.getRecords();
            if(pictureList.size()==0 ){
                log.info("缓存空值");
                caffeienCacheStrategy.setCache(key,JSONUtil.toJsonStr(new Page<PictureVO>()));
                redisCacheStrategy.setCache(key,JSONUtil.toJsonStr(new Page<PictureVO>()),60*15,TimeUnit.SECONDS);
                return ResultUtil.success(new Page<PictureVO>());
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
            pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
            pictureVOPage.setRecords(pictureVOList);
            //给Caffeine缓存数据
//            cache = JSONUtil.toJsonStr(pictureVOPage);
//            log.info("缓存数据");
//            caffeienCacheStrategy.setCache(key, cache);
//            //给redis设置缓存,并设置缓存过期时间
//            redisCacheStrategy.setCache(key, cache, 60*60, TimeUnit.SECONDS);

        }catch (Exception e){
            log.error("分页查询图片数据失败",e);
            ThrowUtil.throwBusinessException(CodeEnum.SYSTEM_ERROR,"系统错误");
        }finally {
            //要确保是自己的锁才释放，获取锁失败的人不能释放
            if(lock.isHeldByCurrentThread()){
                log.info("线程:{},unlock",Thread.currentThread());
                lock.unlock();
            }

        }
        return ResultUtil.success(pictureVOPage,"查询成功");
    }

    //构造缓存Key
    private static String getCacheKey(PictureQueryRequest pictureQueryRequest) {
        //先构造缓存key
        String jsonStr = JSONUtil.toJsonStr(pictureQueryRequest);
        String ma5HexJsonStr = DigestUtils.md5DigestAsHex(jsonStr.getBytes());
        String Key = "picture:pagePictureVOCache"+ma5HexJsonStr;
        return Key;
    }


    /**
     * 分页查询图片（管理员可用）
     * @param pictureQueryRequest
     * @return
     */
    @PostMapping("/page/select/admin")
    @AuthType(userType = UserConstant.ADMIN)
    public BaseResponse<Page<Picture>> selectAdmin(@RequestBody PictureQueryRequest pictureQueryRequest){


        Page<Picture> picturePage = pictureService.selectPage(pictureQueryRequest);

        return ResultUtil.success(picturePage,"查询成功");
    }


    /**
     * 编辑图片
     * @param pictureEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    @AuthType(userType = UserConstant.USER)
    public BaseResponse<PictureVO> editPicture(@RequestBody PictureEditRequest pictureEditRequest,HttpServletRequest request){
        UserVO loginUserVO = TokenUtil.getLoginUserVOFromCookie(request);
        PictureVO pictureVO = pictureService.editPicture(pictureEditRequest, loginUserVO);
        return ResultUtil.success(pictureVO);
    }

    /**
     * 获取图片详情
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    @AuthType(userType = UserConstant.USER)
    public BaseResponse<PictureVO> getPictureVOById(Long id){
        PictureVO pictureVO = pictureService.getPictureVOById(id);

        return ResultUtil.success(pictureVO);
    }

    /**
     * 审核图片
     * @param pictureAuditRequest
     * @param request
     * @return
     */
    @PostMapping("/audit/admin")
    @AuthType(userType = UserConstant.ADMIN)
    public BaseResponse<Boolean> auditPicture(@RequestBody PictureAuditRequest pictureAuditRequest, HttpServletRequest request){
        String token = TokenUtil.getTokenFromCookie(request);
        String loginUserVOJson = stringRedisTemplate.opsForValue().get(token);
        LoginUserVO loginUserVO = JSONUtil.toBean(loginUserVOJson, LoginUserVO.class);
        pictureService.auditPicture(pictureAuditRequest,loginUserVO);
        return ResultUtil.success(true);
    }


    /**
     * 预制标签和分类
     * @return
     */
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtil.success(pictureTagCategory);
    }





}
