package com.zhao.zhaopicturebacked.cos;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.*;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.zhao.zhaopicturebacked.enums.CodeEnum;
import com.zhao.zhaopicturebacked.utils.ThrowUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.File;
import java.io.InputStream;

@Slf4j
@Service
@Getter
public class CosService {

    @Resource
    private CosClientConfig cosClientConfig;

    private String secretId;
    private String secretKey;
    private String bucket;
    private String region;
    private String host;    /**
     * 初始化 COSClient（应用启动时执行一次）
     */
    @PostConstruct
    public void init() {
        secretId = cosClientConfig.getSecretId();
        secretKey = cosClientConfig.getSecretKey();
        bucket = cosClientConfig.getBucket();
        region = cosClientConfig.getRegion();
        host = cosClientConfig.getHost();
    }


    /**
     * 普通的上传图片
     * @param key
     * @param file
     * @return
     */
    public PutObjectResult putPicture(String key , File file) {
        COSClient client = cosClientConfig.getClient();
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(),key,file);
        return client.putObject(putObjectRequest);
    }

    /**
     * 使用文件上传图片并返回原图信息
     * @param key
     * @param file
     * @return
     */
    public PutObjectResult putPictureAndOperation(String key , File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        //构造处理图片的参数
        operation(putObjectRequest);
        COSClient client = cosClientConfig.getClient();
        PutObjectResult putObjectResult = client.putObject(putObjectRequest);
        return putObjectResult;
    }
    private static void operation(PutObjectRequest putObjectRequest) {
        //构造处理图片的参数
        PicOperations picOperations = new PicOperations();
        //设置是否返回原图片的信息
        picOperations.setIsPicInfo(1);
        putObjectRequest.setPicOperations(picOperations);
    }

    public void deletePicture(String key) {
        COSClient client = cosClientConfig.getClient();
        client.deleteObject(cosClientConfig.getBucket(), key);
    }


    /**
     * 使用流上传图片并返回原图信息
     * @param key
     * @param inputStream
     * @param objectMetadata
     * @return
     */
    public PutObjectResult putPictureByStreamAndOperation(String key , InputStream inputStream,ObjectMetadata objectMetadata) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, inputStream,objectMetadata);
        //构造处理图片的参数
        operation(putObjectRequest);
        COSClient client = cosClientConfig.getClient();
        PutObjectResult putObjectResult = client.putObject(putObjectRequest);
        return putObjectResult;
    }


    /**
     * 流式下载
     * @param key
     * @return
     */
    public InputStream getPictureToStream(String key) {
        COSClient client = cosClientConfig.getClient();
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
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
        COSClient client = cosClientConfig.getClient();
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        ObjectMetadata object = client.getObject(getObjectRequest, file);
        return object;
    }





    /**
     * 应用关闭时释放 COSClient 资源
     */
    @PreDestroy
    public void destroy() {
        COSClient cosClient = cosClientConfig.getClient();
        if (cosClient!= null) {
            cosClient.shutdown();
            log.info("COSClient 资源已释放");
        }
    }




}
