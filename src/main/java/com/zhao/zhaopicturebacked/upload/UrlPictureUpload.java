package com.zhao.zhaopicturebacked.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
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

import javax.annotation.Resource;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;


@Slf4j
@Component
public class UrlPictureUpload extends PictureUploadTemplate{

    @Resource
    private CosService cosService;

    @Override
    public void vailPicture(Object inputSource) {
        String fileUrl = (String) inputSource;
        //URL不能为空
        if (ObjUtil.isEmpty(fileUrl)) {
            log.warn("图片URL为空");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"图片URL为空");
        }
        //校验URL是否合法
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            log.error("URL不合法");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"文件地址格式不正确");
        }
        //URL只允许Http或Https协议
        if (!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://")) {
            log.error("仅支持HTTP或HTTPS协议");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"仅支持HTTP或HTTPS协议");
        }
        //URL不能超过1000个字符
        if (fileUrl.length() > 1000) {
            log.error("URL长度超过1000个字符");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"URL长度超过1000个字符");
        }
        HttpResponse response = null;
        try{
            //使用HEAD请求获取图片元信息
            //根据元信息对文件进行格式和大小的校验
            HttpRequest request = HttpUtil.createRequest(Method.HEAD, fileUrl);
            response = request.execute();
            //获取图片的Content-Type
            String contentType = response.header("Content-Type");
            //校验图片的类型
            if(StrUtil.isBlank(contentType)){
                log.error("图片格式缺失");
                ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"图片格式缺失");
            }else{
                //图片格式白名单
                final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/png", "image/gif", "image/webp", "image/avif", "image/jpg");
                if(!ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())){
                    log.error("不允许的图片格式");
                    ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"不允许的图片格式");
                }
            }
            //获取图片的大小
            String contentLength = response.header("Content-Length");
            if(StrUtil.isBlank(contentLength)){
                log.error("图片contentLength缺失");
                ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"图片大小缺失未知");
            }else{
                long size = Long.parseLong(contentLength);
                if(size > 1024 * 1024 * 2){
                    log.error("图片大小超过2M");
                    ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"图片大小超过2M");
                }
            }
        }catch (Exception e){
            log.error("图片URL校验失败");
            ThrowUtil.throwBusinessException(CodeEnum.SYSTEM_ERROR,"图片URL校验失败");
        }finally {
            if(response != null){
                response.close();
            }
        }
    }

    @Override
    public String getKey(Object inputSource, Long userId, PictureUploadRequest pictureUploadRequest) {
        String fileUrl = (String) inputSource;
        //上传图片并返回图片信息
        String name = FileUtil.mainName(fileUrl);
        String suffix = FileUtil.getSuffix(fileUrl);
        Long spaceId = pictureUploadRequest.getSpaceId();
        String key = String.format("/public/%s.%s.%s",userId,name,suffix);
        if(spaceId!=null){
            key = String.format("/%s/%s.%s.%s",spaceId,userId,name,suffix);
        }

        return key;
    }

    @Override
    public PictureInfoResult UploadPicture(Object inputSource, String key) {
        String fileUrl = (String) inputSource;

        PictureInfoResult pictureInfoResult = UploadPictureByUrl(fileUrl,key);
        return pictureInfoResult;
    }

    public PictureInfoResult UploadPictureByUrl(String fileUrl,String key){
        //1.创建空文件
        PictureInfoResult pictureInfoResult = null;
        File tempFile = null;
        try{
            tempFile = FileUtil.createTempFile();
            //2.根据url下载图片到本地文件
            HttpUtil.downloadFile(fileUrl, tempFile);
            //3.将文件上传到COS
            PutObjectResult putObjectResult = cosService.putPictureAndOperation(key, tempFile);
            String name = tempFile.getName();
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if(CollUtil.isNotEmpty(objectList)){

                CIObject compressPicCiObject = objectList.get(0);
                log.info("获取压缩图{}", compressPicCiObject);
                CIObject thumbnailPicCiObject = objectList.get(1);
                log.info("获取缩略图{}",thumbnailPicCiObject);
                pictureInfoResult = getPictureInfoResult(name, compressPicCiObject,thumbnailPicCiObject);
                return pictureInfoResult;

            }
            String format = imageInfo.getFormat();
            int height = imageInfo.getHeight();
            int width = imageInfo.getWidth();
            double scale = NumberUtil.round(width * 1.0 / height, 2).doubleValue();
            long size = FileUtil.size(tempFile);
            //4.将原图信息封装在PictureInfoResult中


            pictureInfoResult = getPictureInfoResult(name, key, format, height, width, scale, size);
        }catch (Exception e){
            log.warn("文件对象转换失败，错误信息{}", e);
            ThrowUtil.throwBusinessException(CodeEnum.SYSTEM_ERROR, "文件对象转换失败");
        }finally {
            FileUtil.del(tempFile);
            log.info("删除临时文件");
        }
        return pictureInfoResult;

    }
}
