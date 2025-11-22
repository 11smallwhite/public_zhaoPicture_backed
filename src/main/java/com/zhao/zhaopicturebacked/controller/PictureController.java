package com.zhao.zhaopicturebacked.controller;

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
import com.zhao.zhaopicturebacked.cache.PicturePageCacheTemplate;
import com.zhao.zhaopicturebacked.cache.PicturePageCaffeineCache;
import com.zhao.zhaopicturebacked.cache.PicturePageRedisCache;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private PicturePageCaffeineCache picturePageCaffeineCache;
    @Resource
    private PicturePageRedisCache picturePageRedisCache;


    @Autowired
    private PictureServiceImpl pictureServiceImpl;

    private final Cache<String, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(1024)
            .maximumSize(10000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();



    /**
     * 通过文件上传图片
     * @param multipartFile
     * @param request
     * @return
     */
    @PostMapping("/upload")
    @AuthType(userType = UserConstant.USER)
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest,HttpServletRequest request) {
        String token = TokenUtil.getTokenFromCookie(request);
        String loginUserVOJson = stringRedisTemplate.opsForValue().get(token);
        log.info("从redis里获取登录用户信息{}",loginUserVOJson);
        if (ObjUtil.isEmpty(loginUserVOJson)){
            log.info("从redis里没有找到用户信息,token已过期或未登录");
            ThrowUtil.throwBusinessException(CodeEnum.NOT_AUTH,"未登录");
        }
        LoginUserVO loginUserVO = JSONUtil.toBean(loginUserVOJson, LoginUserVO.class);
        log.info("Json数据{}转换为对象{}",loginUserVOJson,loginUserVO);
        Long userId = loginUserVO.getId();



        PictureVO pictureVO = pictureService.uploadPicture(multipartFile,pictureUploadRequest, loginUserVO);
        User user = null;
        try {
            user = userService.getById(userId);
            log.info("pictureVO需要填充UserVO，要查数据库得到user");
        }catch (Exception e){
            log.error("查询数据库失败");
            ThrowUtil.throwBusinessException(CodeEnum.SYSTEM_ERROR,"数据库查询出现了错误");
        }
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
        String token = TokenUtil.getTokenFromCookie(request);
        String loginUserVOJson = stringRedisTemplate.opsForValue().get(token);
        log.info("从redis里获取登录用户信息{}",loginUserVOJson);
        if (ObjUtil.isEmpty(loginUserVOJson)){
            log.info("从redis里没有找到用户信息,token已过期或未登录");
            ThrowUtil.throwBusinessException(CodeEnum.NOT_AUTH,"未登录");
        }
        LoginUserVO loginUserVO = JSONUtil.toBean(loginUserVOJson, LoginUserVO.class);
        log.info("Json数据{}转换为对象{}",loginUserVOJson,loginUserVO);
        Long userId = loginUserVO.getId();

        String fileUrl = pictureUploadRequest.getFileUrl();

        PictureVO pictureVO = pictureService.uploadPicture(fileUrl,pictureUploadRequest, loginUserVO);
        User user = null;
        try {
            user = userService.getById(userId);
            log.info("pictureVO需要填充UserVO，要查数据库得到user");
        }catch (Exception e){
            log.error("查询数据库失败");
            ThrowUtil.throwBusinessException(CodeEnum.SYSTEM_ERROR,"数据库查询出现了错误");
        }
        UserVO userVO = UserUtil.getUserVOByUser(user);
        pictureVO.setUserVO(userVO);
        return ResultUtil.success(pictureVO);
    }

    @PostMapping("/upload/batch")
    @AuthType(userType = UserConstant.ADMIN)
    public BaseResponse<Integer> uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, HttpServletRequest request, HttpServletResponse response) {
        if (pictureUploadByBatchRequest == null){
            log.info("参数pictureUploadByBatchRequest为空");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"参数pictureUploadByBatchRequest为空");
        }
        String token = TokenUtil.getTokenFromCookie(request);
        String loginUserVOJson = stringRedisTemplate.opsForValue().get(token);
        if (loginUserVOJson == null){
            log.info("从redis里没有找到用户信息,token已过期或未登录");
            ThrowUtil.throwBusinessException(CodeEnum.NOT_AUTH,"未登录");
        }
        LoginUserVO loginUserVO = JSONUtil.toBean(loginUserVOJson, LoginUserVO.class);
        stringRedisTemplate.expire(token,60*60, TimeUnit.SECONDS);
        //续期cookie
        TokenUtil.setTokenToCookie(token, response);
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
        Long id = deleteRequest.getId();
        String token = TokenUtil.getTokenFromCookie(request);
        //从redis里拿到用户信息
        String loginUserVOJson = stringRedisTemplate.opsForValue().get(token);
        LoginUserVO loginUserVO = JSONUtil.toBean(loginUserVOJson, LoginUserVO.class);
        Long delete = pictureService.deletePicture(id,loginUserVO);
        return ResultUtil.success(delete,"删除成功");
    }


    /**
     * 分页查询图片（用户可用）
     * @param pictureQueryRequest
     * @return
     */
    @PostMapping("/page/select/query")
    public BaseResponse<Page<PictureVO>> select(@RequestBody PictureQueryRequest pictureQueryRequest,HttpServletRequest request){

        String servletPath = request.getServletPath();
        if(!servletPath.contains("admin")){
            pictureQueryRequest.setAuditStatus(AuditStatusEnum.REVIEW_PASS.getCode());
        }


          //先构造缓存key
        String jsonStr = JSONUtil.toJsonStr(pictureQueryRequest);
        String ma5HexJsonStr = DigestUtils.md5DigestAsHex(jsonStr.getBytes());
        String redisKey = "picture:pagePictureVOCache"+ma5HexJsonStr;
        //查找Caffeine缓存
        String cache = LOCAL_CACHE.getIfPresent(redisKey);
        if (cache != null){
            log.info("从Caffeine缓存里获取数据成功");
            Page<PictureVO> pictureVOPage = JSONUtil.toBean(cache, Page.class);
            return ResultUtil.success(pictureVOPage);
        }
        //redis查找缓存
        ValueOperations<String, String> stringStringValueOperations = stringRedisTemplate.opsForValue();
        cache = stringStringValueOperations.get(redisKey);
        if(cache != null){
            log.info("从redis里获取数据成功");
            Page<PictureVO> pictureVOPage = JSONUtil.toBean(cache, Page.class);
            return ResultUtil.success(pictureVOPage);
        }


        //todo 使用分布式锁，只允许一个线程去查询数据库，其他线程等待，这样数据库压力变小，但是用户体验变差，预防了缓存击穿问题
        if (StrUtil.isBlank( cache)){
            //分布式锁{
            //  if(StrUtil.isBlank(cache)){
            //
            //  }
            // }
        }
        Page<Picture> picturePage = pictureService.selectPage(pictureQueryRequest);
        //如果没查到数据，就直接返回空列表,同时也将空列表缓存进redis和Caffeine，防止用户恶意访问不存在的数据,使得数据库压力变大
        List<Picture> pictureList = picturePage.getRecords();
        if(pictureList.size()==0 ){
            LOCAL_CACHE.put(redisKey,JSONUtil.toJsonStr(new Page<PictureVO>()));
            stringStringValueOperations.set(redisKey,JSONUtil.toJsonStr(new Page<PictureVO>()),60*60,TimeUnit.SECONDS);
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
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        pictureVOPage.setRecords(pictureVOList);
        //给Caffeine缓存数据
        cache = JSONUtil.toJsonStr(pictureVOPage);
        log.info("将数据写入Caffeine缓存");
        LOCAL_CACHE.put(redisKey,cache);
        //给redis设置缓存,并设置缓存过期时间
        log.info("将数据缓存进redis");
        stringStringValueOperations.set(redisKey, cache, 60*60, TimeUnit.SECONDS);
        return ResultUtil.success(pictureVOPage,"查询成功");
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
        String token = TokenUtil.getTokenFromCookie(request);
        String loginUserVOJson = stringRedisTemplate.opsForValue().get(token);
        LoginUserVO loginUserVO = JSONUtil.toBean(loginUserVOJson, LoginUserVO.class);
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
