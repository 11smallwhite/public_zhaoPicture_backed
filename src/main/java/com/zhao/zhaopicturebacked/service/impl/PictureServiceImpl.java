package com.zhao.zhaopicturebacked.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.zhao.zhaopicturebacked.common.UserConstant;
import com.zhao.zhaopicturebacked.config.CosClientConfig;
import com.zhao.zhaopicturebacked.cos.CosService;
import com.zhao.zhaopicturebacked.cos.PictureInfoResult;
import com.zhao.zhaopicturebacked.domain.Picture;
import com.zhao.zhaopicturebacked.domain.User;
import com.zhao.zhaopicturebacked.enums.AuditStatusEnum;
import com.zhao.zhaopicturebacked.enums.CodeEnum;
import com.zhao.zhaopicturebacked.mapper.PictureMapper;
import com.zhao.zhaopicturebacked.model.LoginUserVO;
import com.zhao.zhaopicturebacked.model.PictureVO;
import com.zhao.zhaopicturebacked.model.UserVO;
import com.zhao.zhaopicturebacked.request.picture.PictureAuditRequest;
import com.zhao.zhaopicturebacked.request.picture.PictureEditRequest;
import com.zhao.zhaopicturebacked.request.picture.PictureQueryRequest;
import com.zhao.zhaopicturebacked.service.PictureService;
import com.zhao.zhaopicturebacked.service.UserService;

import com.zhao.zhaopicturebacked.utils.ThrowUtil;
import com.zhao.zhaopicturebacked.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
* @author Vip
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2025-11-05 09:50:11
*/
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

    @Resource
    private CosService cosService;

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private UserService userService;

    /**
     * 上传图片
     * @param multipartFile
     * @param pictureId
     * @return
     */
    @Override
    public PictureVO uploadPicture(MultipartFile multipartFile,Long pictureId,LoginUserVO loginUserVO) {
        if (ObjUtil.isEmpty(loginUserVO.getId())){
            log.warn("userId参数为空");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"id参数为空");
        }
        //如果PictureId不为空，则证明是更新图片，需要检查图片在数据库是否存在,并且只允许图片的创建者更新
        if(ObjUtil.isNotEmpty(pictureId)){
            Picture oldPicture = this.getById(pictureId);
            if (ObjUtil.isEmpty(oldPicture)){
                log.warn("图片不存在");
                ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"图片不存在");
            }
            if (!oldPicture.getUserId().equals(loginUserVO.getId())){
                log.warn("不是图片的创建者");
                ThrowUtil.throwBusinessException(CodeEnum.NOT_AUTH,"无权限");
            }
            //如果图片存在于数据库，就说明图片也存在在对象存储上，需要进行删除
            String key = getKey(oldPicture.getUserId(),oldPicture.getpName(),oldPicture.getpFormat());

            cosService.deletePicture(key);
        }
        //校验图片文件
        vailPictureFile(multipartFile);
        //上传图片并返回图片信息
        PictureInfoResult pictureInfoResult = UploadPictureByStream(multipartFile,loginUserVO.getId());
        //将图片存入数据库
        Picture picture = new Picture();
        picture.setpUrl(pictureInfoResult.getUrl());
        picture.setpName(pictureInfoResult.getName());
        picture.setpIntroduction("该图片很懒，什么都没留下");
        picture.setpCategory("未分类");
        picture.setpSize(pictureInfoResult.getSize());
        picture.setpWidth(pictureInfoResult.getWidth());
        picture.setpHeight(pictureInfoResult.getHeight());
        picture.setpScale(pictureInfoResult.getScale());
        picture.setpFormat(pictureInfoResult.getFormat());
        picture.setUserId(loginUserVO.getId());
        fillAcditColumn(picture,loginUserVO);
        if(pictureId!=null){
            picture.setId(pictureId);
        }
        //todo 这里要使用自动填充,picture的createTime等字段在插入时自动填充会picture里
        boolean save = this.saveOrUpdate( picture);
        if (!save) {
            log.error("图片保存失败");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"图片保存失败");
        }
        log.info("picture:{}",picture);
        //5.返回PictureVO
        PictureVO pictureVO = getPictureVOByPicture(picture);

        return pictureVO;
    }

    public void fillAcditColumn(Picture picture,LoginUserVO loginUserVO){
        if (loginUserVO.getUserType()== UserConstant.ADMIN){
            picture.setAuditorId(loginUserVO.getId());
            picture.setAuditTime(new Date());
            picture.setAuditMsg("审核通过");
            picture.setAuditStatus(AuditStatusEnum.REVIEW_PASS.getCode());
        }else{
            picture.setAuditStatus(AuditStatusEnum.REVIEWING.getCode());
        }


    }

    /**
     * 使用文件上传图片并返回原图信息的具体逻辑
     * @param multipartFile
     * @return
     */
    public PictureInfoResult UploadPictureByFile(MultipartFile multipartFile) {
        log.info("执行方法UploadPicture,参数为{}和{}", multipartFile);
        String originalFilename = multipartFile.getOriginalFilename();
        //2.构建图片存储目录路径key,并将multipartFile转换成File
        //2.1 构造一个  /picture/id/时间/uuid.文件名的key
        String key = String.format("/%s/%s/%s.%s", "public", System.currentTimeMillis(), UUID.randomUUID(), originalFilename);
        //2.2将multipartFile转换成File
        PictureInfoResult pictureInfoResult = null;
        File file = null;
        try {
            file = FileUtil.createTempFile();
            multipartFile.transferTo(file);
            log.info("文件对象转换成功");
            //3.上传图片到COS并返回原图信息
            PutObjectResult putObjectResult = cosService.putPictureAndOperation(key, file);
            //4.封装原图信息,存放数据库
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            String format = imageInfo.getFormat();
            int height = imageInfo.getHeight();
            int width = imageInfo.getWidth();
            double scale = NumberUtil.round(width * 1.0 / height, 2).doubleValue();
            long size = FileUtil.size(file);
            //5.将原图信息封装在PictureInfoResult中
            pictureInfoResult = getPictureInfoResult(originalFilename, key, format, height, width, scale, size);
        } catch (IOException e) {
            log.warn("文件对象转换失败，错误信息{}", e);
            ThrowUtil.throwBusinessException(CodeEnum.SYSTEM_ERROR, "文件对象转换失败");
        } finally {
            FileUtil.del(file);
            log.info("删除临时文件");
        }
        return pictureInfoResult;
    }




    /**
     * 使用流上传图片并返回原图信息
     * @param multipartFile
     * @return
     */
    public PictureInfoResult UploadPictureByStream(MultipartFile multipartFile,Long userId) {
        log.info("执行方法UploadPictureByStream,参数为{}和{}", multipartFile);
        String originalFilename = multipartFile.getOriginalFilename();
        String name = FileUtil.mainName(originalFilename);
        String suffix = FileUtil.getSuffix(originalFilename);
        String key = getKey(userId,name,suffix);
        PictureInfoResult pictureInfoResult = null;
        try{
            //获取到文件的流
            InputStream inputStream = multipartFile.getInputStream();
            ObjectMetadata objectMetadata = new ObjectMetadata();
            //设置流的长度
            objectMetadata.setContentLength(multipartFile.getSize());
            objectMetadata.setContentType(multipartFile.getContentType());
            PutObjectResult putObjectResult = cosService.putPictureByStreamAndOperation(key, inputStream, objectMetadata);
            //4.封装原图信息,存放数据库
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            //这里如果使用imageInfo的format，会出现的一种情况是，imageInfo的format保存的是jpeg，所以数据库存的也是jpeg，但是对象存储上的文件后缀是jpg，这会在删除时出现错误。所以这里需要使用FileUtil.getSuffix(originalFilename)获取文件后缀
            //String format = imageInfo.getFormat();
            String format = FileUtil.getSuffix(originalFilename);
            int height = imageInfo.getHeight();
            int width = imageInfo.getWidth();
            double scale = NumberUtil.round(width * 1.0 / height, 2).doubleValue();
            long size = multipartFile.getSize();
            //5.将原图信息封装在PictureInfoResult中
            pictureInfoResult = getPictureInfoResult(originalFilename, key, format, height, width, scale, size);
        }catch (IOException e){
            log.warn("获取文件的流对象失败，错误信息{}", e);
            ThrowUtil.throwBusinessException(CodeEnum.SYSTEM_ERROR, "获取文件的流对象失败");
        }
        return pictureInfoResult;
    }


    /**
     * 删除图片
     * @param id
     * @param loginUserVO
     * @return
     */
    @Override
    public Long deletePicture(Long id,LoginUserVO loginUserVO) {
        //1.校验id
        if (ObjUtil.isEmpty(id)){
            log.warn("id参数为空");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"id参数为空");
        }
        //2.查询被删除的数据是否存在
        Picture picture = this.getById(id);
        if (ObjUtil.isEmpty(picture)){
            log.warn("id对应的数据不存在");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"id对应数据不存在");
        }
        Long userId = picture.getUserId();
        Integer userType = loginUserVO.getUserType();
        Long loginUserVOId = loginUserVO.getId();
        //2.校验用户有无权限删除这张图片
        if(userId!=loginUserVOId&&userType!=1){
            log.warn("用户没有权限删除这张图片");
            ThrowUtil.throwBusinessException(CodeEnum.NOT_AUTH,"无权限");
        }

        boolean b = this.removeById(id);
        if (!b){
            log.warn("删除数据失败");
            ThrowUtil.throwBusinessException(CodeEnum.SYSTEM_ERROR,"删除数据失败");
        }
        //todo
        //3.删除对象存储数据
        //得到要删除数据的key
        String key = getKey(userId, picture.getpName(), picture.getpFormat());
        try {
            log.info("删除对象存储里的图片信息");
            cosService.deletePicture(key);
        }catch (Exception e){
            log.warn("删除对象存储里的图片信息失败");
            ThrowUtil.throwBusinessException(CodeEnum.SYSTEM_ERROR,"删除对象存储里的图片信息失败");
        }
        return id;
    }

    /**
     * 查询图片
     * @param pictureQueryRequest
     * @return
     */
    @Override
    public Page<Picture> selectPage(PictureQueryRequest pictureQueryRequest) {
        Long id = pictureQueryRequest.getId();
        Long userId = pictureQueryRequest.getUserId();
        String searchText = pictureQueryRequest.getSearchText();
        String pCategory = pictureQueryRequest.getPCategory();
        List<String> pTags = pictureQueryRequest.getPTags();
        Long pSize = pictureQueryRequest.getPSize();
        Integer pWidth = pictureQueryRequest.getPWidth();
        Integer pHeight = pictureQueryRequest.getPHeight();
        Double pScale = pictureQueryRequest.getPScale();
        Integer auditStatus = pictureQueryRequest.getAuditStatus();
        Long auditId = pictureQueryRequest.getAuditId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Integer pageNum = pictureQueryRequest.getPageNum();
        Integer pageSize = pictureQueryRequest.getPageSize();

        QueryWrapper<Picture> pictureQueryWrapper = new QueryWrapper<>();
        if (id!=null){
            if(id>0){
                pictureQueryWrapper.eq("id",id);
            }

        }
        pictureQueryWrapper.eq(ObjUtil.isNotEmpty(userId)&&userId>0,"user_id",userId);
        pictureQueryWrapper.and(ObjUtil.isNotEmpty(searchText),wrapper -> wrapper.like("p_name", searchText).or().like("p_introduction", searchText));
        pictureQueryWrapper.like( ObjUtil.isNotEmpty(pCategory),"p_category", pCategory);

        if(pTags!=null){
            for (String tag : pTags){
                pictureQueryWrapper.like( ObjUtil.isNotEmpty( tag),"p_tags", "\""+tag+"\"");
            }
        }

        pictureQueryWrapper.eq( ObjUtil.isNotEmpty(pSize)&&pSize>0,"p_size", pSize);
        pictureQueryWrapper.eq( ObjUtil.isNotEmpty(pWidth)&&pWidth>0,"p_width", pWidth);
        pictureQueryWrapper.eq( ObjUtil.isNotEmpty(pHeight)&&pHeight>0,"p_height", pHeight);
        pictureQueryWrapper.eq( ObjUtil.isNotEmpty(pScale)&&pScale>0,"p_scale", pScale);

        pictureQueryWrapper.eq(ObjUtil.isNotEmpty(auditStatus),"audit_status", auditStatus);
        pictureQueryWrapper.eq( ObjUtil.isNotEmpty(auditId)&&auditId>0,"audit_id", auditId);
        String s = convertFieldToColumn(sortField);
        pictureQueryWrapper.orderBy(ObjUtil.isNotNull(s), sortOrder.equals("asc"), s);
        Page<Picture> picturePage = this.page(new Page<>(pageNum, pageSize), pictureQueryWrapper);
        return picturePage;
    }

    /**
     * 编辑图片
     * @param pictureEditRequest
     * @param loginUserVO
     * @return
     */
    @Override
    public PictureVO editPicture(PictureEditRequest pictureEditRequest, LoginUserVO loginUserVO) {
        Long id = pictureEditRequest.getId();
        String pName = pictureEditRequest.getPName();
        String pIntroduction = pictureEditRequest.getPIntroduction();
        String pCategory = pictureEditRequest.getPCategory();
        List<String> pTags = pictureEditRequest.getPTags();
        if(ObjUtil.isEmpty(id)){
            log.warn("id参数为空");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"id参数为空");
        }
        //1.查看图片是否存在
        Picture oldPicture = this.getById(id);
        if(ObjUtil.isEmpty(oldPicture)){
            log.warn("id对应的图片不存在");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"id对应的图片不存在");
        }
        Long userId = oldPicture.getUserId();
        //校验是否有权限编辑图片
        if(!userId.equals(loginUserVO.getId()) && loginUserVO.getUserType()!=1){
            log.warn("用户没有权限编辑图片");
            ThrowUtil.throwBusinessException(CodeEnum.NOT_AUTH,"无权限");
        }
        //更新图片
        Picture picture = new Picture();
        picture.setId(id);
        picture.setpName(pName);
        picture.setpIntroduction(pIntroduction);
        picture.setpCategory(pCategory);
        String tagsJson = JSONUtil.toJsonStr(pTags);
        picture.setpTags(tagsJson);
        fillAcditColumn(picture,loginUserVO);
        boolean b = this.updateById(picture);
        if (!b){
            log.warn("更新图片失败");
            ThrowUtil.throwBusinessException(CodeEnum.SYSTEM_ERROR,"更新图片失败");
        }
        PictureVO pictureVO = getPictureVOByPicture(oldPicture);

        return pictureVO;
    }

    /**
     * 获取单个图片信息
     * @param id
     * @return
     */
    @Override
    public PictureVO getPictureVOById(Long id) {
        if(id<0||ObjUtil.isEmpty(id)){
            log.warn("id参数错误");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"id参数错误");
        }
        Picture picture = this.getById(id);
        if(ObjUtil.isEmpty(picture)){
            log.warn("id对应的图片不存在");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"id对应的图片不存在");
        }
        PictureVO pictureVO = getPictureVOByPicture(picture);
        return pictureVO;
    }

    /**
     * 管理员审核信息
     * @param pictureAuditRequest
     */
    @Override
    public void auditPicture(PictureAuditRequest pictureAuditRequest, LoginUserVO loginUserVO) {
        Long pictureId = pictureAuditRequest.getPictureId();
        Integer audioStatus = pictureAuditRequest.getAuditStatus();
        String audioMsg = pictureAuditRequest.getAuditMsg();
        if(ObjUtil.isEmpty(pictureId)||ObjUtil.isEmpty(audioStatus)){
            log.warn("参数错误");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"参数错误");
        }

        Picture oldPicture = this.getById(pictureId);
        if(oldPicture.getAuditStatus().equals(AuditStatusEnum.getByCode(audioStatus))){
            log.warn("图片已审核");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"请勿重复审核");
        }

        Picture picture = new Picture();
        picture.setAuditMsg(audioMsg);
        picture.setAuditStatus(audioStatus);
        picture.setId(pictureId);
        picture.setAuditTime(new Date());
        picture.setAuditorId(loginUserVO.getId());
        boolean b = this.updateById(picture);
        if (!b){
            log.warn("更新图片失败");
            ThrowUtil.throwBusinessException(CodeEnum.SYSTEM_ERROR,"审核失败");
        }
        return ;

    }


    public String getKey(Long userId,String name,String format){
        return String.format("/public/%s.%s.%s",userId,name,format);
    }

    // 字段名映射方法
    private String convertFieldToColumn(String fieldName) {
        if (ObjUtil.isEmpty(fieldName)) {
            return null;
        }

        switch (fieldName) {
            case "createTime":
                return "create_time";
            case "updateTime":
                return "update_time";
            case "editTime":
                return "edit_time";
            case "pName":
                return "p_name";
            case "pIntroduction":
                return "p_introduction";
            case "pCategory":
                return "p_category";
            case "pTags":
                return "p_tags";
            default:
                return fieldName; // 如果已经是数据库字段名，直接返回
        }
    }
    public PictureVO getPictureVOByPicture(Picture picture){
        PictureVO pictureVO = new PictureVO();
        BeanUtils.copyProperties(picture,pictureVO);
        if (ObjUtil.isEmpty(pictureVO)){
            log.warn("Picture转换为PictureVO失败");
            ThrowUtil.throwBusinessException(CodeEnum.SYSTEM_ERROR,"Picture转换为PictureVO失败");
        }
        String tagsJson = picture.getpTags();
        List<String> tags = JSONUtil.toList(tagsJson, String.class);
        pictureVO.setPTags(tags);
        User user = userService.getById(picture.getUserId());
        UserVO userVO = UserUtil.getUserVOByUser(user);
        pictureVO.setUserVO(userVO);
        return pictureVO;
    }


    public void vailPictureFile(MultipartFile multipartFile){
        //1.校验图片文件
        //1.0图片不能为空
        if (multipartFile.isEmpty()) {
            log.info("图片文件为空");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"不允许上传空文件");
        }
        //1.1图片大小不能超过2M
        long size = multipartFile.getSize();
        if (size > 1024 * 1024 * 2) {
            log.info("图片大小不能超过2M");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"图片大小不能超过2M");
        }
        //1.2允许的图片格式
        String originalFilename = multipartFile.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        //白名单，允许的图片后缀
        final ArrayList<String> pSuffix = new ArrayList<>();
        pSuffix.add("png");
        pSuffix.add("jpg");
        pSuffix.add("jpeg");
        pSuffix.add("avif");
        if (!pSuffix.contains(suffix)) {
            log.info("图片格式错误");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"图片格式错误");
        }
    }

    /**
     * 封装原图信息
     * @param originalFilename
     * @param key
     * @param format
     * @param height
     * @param width
     * @param scale
     * @param size
     * @return
     */
    private PictureInfoResult getPictureInfoResult(String originalFilename, String key, String format, int height, int width, double scale, long size) {
        PictureInfoResult pictureInfoResult;
        pictureInfoResult = new PictureInfoResult();
        pictureInfoResult.setFormat(format);
        pictureInfoResult.setWidth(width);
        pictureInfoResult.setHeight(height);
        pictureInfoResult.setScale(scale);
        pictureInfoResult.setSize(size);
        pictureInfoResult.setUrl(cosClientConfig.getHost() + "/" + key);
        pictureInfoResult.setName(FileUtil.getPrefix(originalFilename));
        return pictureInfoResult;
    }



}




