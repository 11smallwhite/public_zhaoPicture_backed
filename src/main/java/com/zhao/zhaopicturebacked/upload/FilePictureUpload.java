package com.zhao.zhaopicturebacked.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import com.zhao.zhaopicturebacked.cos.CosService;
import com.zhao.zhaopicturebacked.cos.PictureInfoResult;
import com.zhao.zhaopicturebacked.enums.CodeEnum;
import com.zhao.zhaopicturebacked.request.picture.PictureUploadRequest;
import com.zhao.zhaopicturebacked.utils.ThrowUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;


import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class FilePictureUpload extends PictureUploadTemplate{


    @Resource
    private CosService cosService;

    @Override
    public void vailPicture(Object inputSource) {
        //1.校验图片文件
        //1.0图片不能为空
        MultipartFile multipartFile = (MultipartFile) inputSource;
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



    @Override
    public String getKey(Object inputSource, Long userId, PictureUploadRequest pictureUploadRequest) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        String originalFilename = multipartFile.getOriginalFilename();
        String name = FileUtil.mainName(originalFilename);
        String suffix = FileUtil.getSuffix(originalFilename);
        Long spaceId = pictureUploadRequest.getSpaceId();
        String key = String.format("/public/%s.%s.%s",userId,name,suffix);
        if(spaceId!=null){
            key = String.format("/%s/%s.%s.%s",spaceId,userId,name,suffix);
        }

        return key;
    }

    @Override
    public PictureInfoResult UploadPicture(Object inputSource, String key) {

        MultipartFile multipartFile = (MultipartFile) inputSource;
        PictureInfoResult pictureInfoResult = UploadPictureByStream(multipartFile,key);
        return pictureInfoResult;
    }




    /**
     * 使用流上传图片并返回原图信息
     * @param multipartFile
     * @return
     */
    public PictureInfoResult UploadPictureByStream(MultipartFile multipartFile,String key) {
        log.info("执行方法UploadPictureByStream,参数为{}和{}", multipartFile);

        PictureInfoResult pictureInfoResult = null;
        try{
            //获取到文件的流
            InputStream inputStream = multipartFile.getInputStream();
            ObjectMetadata objectMetadata = new ObjectMetadata();
            //设置流的长度
            objectMetadata.setContentLength(multipartFile.getSize());
            objectMetadata.setContentType(multipartFile.getContentType());
            PutObjectResult putObjectResult = cosService.putPictureByStreamAndOperation(key, inputStream, objectMetadata,multipartFile);

            //4.封装原图信息,存放数据库
            String originalFilename = multipartFile.getOriginalFilename();
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if(CollUtil.isNotEmpty(objectList)){

                CIObject compressPicCiObject = objectList.get(0);
                log.info("获取压缩图{}", compressPicCiObject);
                CIObject thumbnailPicCiObject = compressPicCiObject;
                if(objectList.size()>1){
                    thumbnailPicCiObject = objectList.get(1);
                    log.info("获取缩略图{}",thumbnailPicCiObject);
                }else{
                    log.info("没有缩略图，缩略图就是压缩图");
                }

                pictureInfoResult = getPictureInfoResult(originalFilename, compressPicCiObject,thumbnailPicCiObject);
                return pictureInfoResult;

            }
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

}
