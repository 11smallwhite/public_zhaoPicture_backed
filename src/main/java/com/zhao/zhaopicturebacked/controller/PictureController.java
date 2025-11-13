package com.zhao.zhaopicturebacked.controller;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhao.zhaopicturebacked.annotation.AuthType;
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
import com.zhao.zhaopicturebacked.request.picture.PictureAudioRequest;
import com.zhao.zhaopicturebacked.request.picture.PictureEditRequest;
import com.zhao.zhaopicturebacked.request.picture.PictureQueryRequest;
import com.zhao.zhaopicturebacked.request.picture.PictureUploadRequest;
import com.zhao.zhaopicturebacked.service.PictureService;
import com.zhao.zhaopicturebacked.service.UserService;
import com.zhao.zhaopicturebacked.service.impl.PictureServiceImpl;
import com.zhao.zhaopicturebacked.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    @Autowired
    private PictureServiceImpl pictureServiceImpl;



    /**
     * 上传图片
     * @param multipartFile
     * @param request
     * @return
     */
    @PostMapping("/upload")
    @AuthType(userType = UserConstant.ADMIN)
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest,HttpServletRequest request) {
        Long pictureId = pictureUploadRequest.getId();
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
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile,pictureId, loginUserVO);
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
        Page<Picture> picturePage = pictureService.selectPage(pictureQueryRequest);
        //如果没查到数据，就直接返回空列表
        List<Picture> pictureList = picturePage.getRecords();
        if(pictureList.size()==0 ){
            return ResultUtil.success(new Page<>());
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
     * @param pictureAudioRequest
     * @param request
     * @return
     */
    @PostMapping("/audit/admin")
    @AuthType(userType = UserConstant.ADMIN)
    public BaseResponse<Boolean> auditPicture(@RequestBody PictureAudioRequest pictureAudioRequest,HttpServletRequest request){
        String token = TokenUtil.getTokenFromCookie(request);
        String loginUserVOJson = stringRedisTemplate.opsForValue().get(token);
        LoginUserVO loginUserVO = JSONUtil.toBean(loginUserVOJson, LoginUserVO.class);
        pictureService.auditPicture(pictureAudioRequest,loginUserVO);
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
