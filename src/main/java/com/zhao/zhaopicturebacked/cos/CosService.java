package com.zhao.zhaopicturebacked.cos;

import ch.qos.logback.classic.spi.EventArgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.*;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.zhao.zhaopicturebacked.config.CosClientConfig;
import com.zhao.zhaopicturebacked.enums.CodeEnum;
import com.zhao.zhaopicturebacked.utils.ThrowUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
public class CosService {

    @Resource
    private CosClientConfig cosclientConfig;


    /**
     * 普通的上传图片
     * @param key
     * @param file
     * @return
     */
    public PutObjectResult putPicture(String key , File file) {
        COSClient client = cosclientConfig.getClient();
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosclientConfig.getBucket(),key,file);
        return client.putObject(putObjectRequest);
    }

    /**
     * 使用文件上传图片并返回原图信息
     * @param key
     * @param file
     * @return
     */
    public PutObjectResult putPictureAndOperation(String key , File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosclientConfig.getBucket(), key, file);
        //构造处理图片的参数
        PicOperations picOperations = new PicOperations();
        //设置是否返回原图片的信息
        picOperations.setIsPicInfo(1);
        putObjectRequest.setPicOperations(picOperations);
        COSClient client = cosclientConfig.getClient();
        PutObjectResult putObjectResult = client.putObject(putObjectRequest);
        return putObjectResult;
    }

    public void deletePicture(String key) {
        COSClient client = cosclientConfig.getClient();
        client.deleteObject(cosclientConfig.getBucket(), key);
    }


    /**
     * 使用流上传图片并返回原图信息
     * @param key
     * @param inputStream
     * @param objectMetadata
     * @return
     */
    public PutObjectResult putPictureByStreamAndOperation(String key , InputStream inputStream,ObjectMetadata objectMetadata) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosclientConfig.getBucket(), key, inputStream,objectMetadata);
        //构造处理图片的参数
        PicOperations picOperations = new PicOperations();
        //设置是否返回原图片的信息
        picOperations.setIsPicInfo(1);
        putObjectRequest.setPicOperations(picOperations);
        COSClient client = cosclientConfig.getClient();
        PutObjectResult putObjectResult = client.putObject(putObjectRequest);
        return putObjectResult;
    }


    /**
     * 流式下载
     * @param key
     * @return
     */
    public InputStream getPictureToStream(String key) {
        COSClient client = cosclientConfig.getClient();
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosclientConfig.getBucket(), key);
        InputStream cosObjectInputStream =null;
        try{
            COSObject cosObject = client.getObject(getObjectRequest);
            cosObjectInputStream = cosObject.getObjectContent();
        }catch (Exception e){
            log.warn("下载图片失败");
            ThrowUtil.throwBusinessException(CodeEnum.SYSTEM_ERROR,"下载图片失败");
        }
        return cosObjectInputStream;
    }

    /**
     * 文件下载
     * @param key
     * @param file
     * @return
     */
    public ObjectMetadata getPicturetoFile(String key,File file){
        COSClient client = cosclientConfig.getClient();
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosclientConfig.getBucket(), key);
        ObjectMetadata object = client.getObject(getObjectRequest, file);
        return object;
    }


}
