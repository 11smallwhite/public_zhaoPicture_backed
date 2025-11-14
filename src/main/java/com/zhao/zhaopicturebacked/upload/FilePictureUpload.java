package com.zhao.zhaopicturebacked.upload;

import cn.hutool.core.io.FileUtil;
import com.zhao.zhaopicturebacked.cos.CosService;
import com.zhao.zhaopicturebacked.cos.PictureInfoResult;
import com.zhao.zhaopicturebacked.enums.CodeEnum;
import com.zhao.zhaopicturebacked.utils.ThrowUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.ArrayList;

@Slf4j
public class FilePictureUpload extends PictureUploadTemplate{


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
    public String getKey(Object inputSource, Long userId) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        String originalFilename = multipartFile.getOriginalFilename();
        String name = FileUtil.mainName(originalFilename);
        String suffix = FileUtil.getSuffix(originalFilename);
        String key = String.format("/public/%s.%s.%s",userId,name,suffix);
        return key;
    }

    @Override
    public PictureInfoResult UploadPicture(Object inputSource, String key) {

        MultipartFile multipartFile = (MultipartFile) inputSource;
        PictureInfoResult pictureInfoResult = UploadPictureByStream(multipartFile,key);
        return pictureInfoResult;
    }
}
