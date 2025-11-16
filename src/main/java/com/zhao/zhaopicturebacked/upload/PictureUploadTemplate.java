package com.zhao.zhaopicturebacked.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.http.HttpUtil;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.zhao.zhaopicturebacked.common.UserConstant;
import com.zhao.zhaopicturebacked.cos.CosService;
import com.zhao.zhaopicturebacked.cos.PictureInfoResult;
import com.zhao.zhaopicturebacked.domain.Picture;
import com.zhao.zhaopicturebacked.enums.AuditStatusEnum;
import com.zhao.zhaopicturebacked.enums.CodeEnum;
import com.zhao.zhaopicturebacked.model.LoginUserVO;
import com.zhao.zhaopicturebacked.utils.ThrowUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

@Slf4j
public abstract class PictureUploadTemplate {



   @Resource
   private CosService cosService;



    public Picture uploadPicture(Object inputSource, Long pictureId, LoginUserVO loginUserVO){

        Long userId = loginUserVO.getId();

        vailPicture(inputSource);

        String key = getKey(inputSource, userId);

        PictureInfoResult pictureInfoResult = UploadPicture(inputSource, key);

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
        if (loginUserVO.getUserType()== UserConstant.ADMIN){
            picture.setAuditorId(loginUserVO.getId());
            picture.setAuditTime(new Date());
            picture.setAuditMsg("审核通过");
            picture.setAuditStatus(AuditStatusEnum.REVIEW_PASS.getCode());
        }else{
            picture.setAuditStatus(AuditStatusEnum.REVIEWING.getCode());
        }
        if(pictureId!=null){
            picture.setId(pictureId);
        }
        return picture;

    }



    public abstract void vailPicture(Object inputSource);

    public abstract String getKey(Object inputSource,Long userId);

    public abstract PictureInfoResult UploadPicture(Object inputSource,String key);



    /**
     * 使用文件上传图片并返回原图信息的具体逻辑
     * @param multipartFile
     * @return
     */
    public PictureInfoResult UploadPictureByFile(MultipartFile multipartFile, String key) {
        log.info("执行方法UploadPicture,参数为{}和{}", multipartFile);
        String originalFilename = multipartFile.getOriginalFilename();
        //2.构建图片存储目录路径key,并将multipartFile转换成File
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
    public PictureInfoResult getPictureInfoResult(String originalFilename, String key, String format, int height, int width, double scale, long size) {
        PictureInfoResult pictureInfoResult;
        pictureInfoResult = new PictureInfoResult();
        pictureInfoResult.setFormat(format);
        pictureInfoResult.setWidth(width);
        pictureInfoResult.setHeight(height);
        pictureInfoResult.setScale(scale);
        pictureInfoResult.setSize(size);
        pictureInfoResult.setUrl(cosService.getHost() + "/" + key);
        pictureInfoResult.setName(FileUtil.getPrefix(originalFilename));
        return pictureInfoResult;
    }








}
