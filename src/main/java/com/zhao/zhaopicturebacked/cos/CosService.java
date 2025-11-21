package com.zhao.zhaopicturebacked.cos;

import cn.hutool.core.io.FileUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.*;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.zhao.zhaopicturebacked.enums.CodeEnum;
import com.zhao.zhaopicturebacked.utils.ThrowUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;

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
        operation(putObjectRequest,file);
        COSClient client = cosClientConfig.getClient();
        PutObjectResult putObjectResult = client.putObject(putObjectRequest);
        return putObjectResult;
    }
    //数据万象图片处理规则
    private void operation(PutObjectRequest putObjectRequest,File file) {
        //构造处理图片的参数
        PicOperations picOperations = new PicOperations();
        //设置是否返回原图片的信息
        picOperations.setIsPicInfo(1);
        ArrayList<PicOperations.Rule> rules = new ArrayList<>();

        //设置压缩(格式转换)规则
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setRule("imageMogr2/format/webp");
        compressRule.setBucket(cosClientConfig.getBucket());
        compressRule.setFileId(FileUtil.mainName(file)+".webp");
        //只有当图片大于20kb时才设置缩放
        if(file.length()>20*1024){
            //设置缩放规则
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            thumbnailRule.setBucket(cosClientConfig.getBucket());
            thumbnailRule.setFileId(FileUtil.mainName(file)+"_thumbnail"+FileUtil.getSuffix(file));
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>",128,128));
            rules.add(thumbnailRule);
        }
        rules.add( compressRule);
        picOperations.setRules( rules);
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
    public PutObjectResult putPictureByStreamAndOperation(String key , InputStream inputStream, ObjectMetadata objectMetadata, MultipartFile multipartFile) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, inputStream,objectMetadata);
        //构造处理图片的参数
        File tempFile = FileUtil.createTempFile();
        PutObjectResult putObjectResult = null;
        try{
            multipartFile.transferTo(tempFile);
            operation(putObjectRequest,tempFile);
            COSClient client = cosClientConfig.getClient();
            putObjectResult = client.putObject(putObjectRequest);
        }catch (Exception e){
            log.warn("图片转存失败");
            ThrowUtil.throwBusinessException(CodeEnum.SYSTEM_ERROR,"图片转存失败");
        }finally {
            FileUtil.del(tempFile);
        }


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
