package com.zhao.zhaopicturebacked.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.http.HttpUtil;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.zhao.zhaopicturebacked.common.UserConstant;
import com.zhao.zhaopicturebacked.cos.CosService;
import com.zhao.zhaopicturebacked.cos.PictureInfoResult;
import com.zhao.zhaopicturebacked.domain.Picture;
import com.zhao.zhaopicturebacked.domain.Space;
import com.zhao.zhaopicturebacked.enums.AuditStatusEnum;
import com.zhao.zhaopicturebacked.enums.CodeEnum;
import com.zhao.zhaopicturebacked.model.LoginUserVO;
import com.zhao.zhaopicturebacked.model.UserVO;
import com.zhao.zhaopicturebacked.request.picture.PictureUploadRequest;
import com.zhao.zhaopicturebacked.service.SpaceService;
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
@Component
public abstract class PictureUploadTemplate {



   @Resource
   private CosService cosService;

   @Resource
   private SpaceService spaceService;



    public Picture uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, UserVO loginUserVO,Picture oldPicture){

        Long pictureId = pictureUploadRequest.getId();

        Long userId = loginUserVO.getId();




        vailPicture(inputSource);

        Long spaceId = pictureUploadRequest.getSpaceId();

        //如果spaceId不为空，则证明是上传图片到空间，需要检查空间在数据库是否存在,并且只允许空间创建者上传图片
        Space space = null;
        if(ObjUtil.isNotEmpty(spaceId)){
            space = spaceService.getById(spaceId);
            if(ObjUtil.isEmpty(space)){
                log.warn("空间不存在");
                ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"空间不存在");
            }
            if(!space.getUserId().equals(loginUserVO.getId())){
                log.warn("不是空间创建者");
                ThrowUtil.throwBusinessException(CodeEnum.NOT_AUTH,"无权限");
            }
            if(ObjUtil.isNotEmpty(oldPicture)){
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, spaceId)
                        .setSql("total_size = total_size - " + oldPicture.getpSize())
                        .setSql("total_count = total_count - 1")
                        .update();
                if (!update){
                    log.warn("更新空间额度失败");
                    ThrowUtil.throwBusinessException(CodeEnum.SYSTEM_ERROR,"更新空间额度失败");
                }
            }

        }
        //这里会有并发问题，如果用户在临界超额的时候，同时发送了多张图片，可能会出现超额情况
        //但是如果要完全阻止这种并发问题的话，我们就得用上锁，并且要自己计算文件的大小，但是由于我们的上传图片的逻辑本身就挺复杂了，所以我们考虑从业务层面上优化
        //单张图片最大才2M，即使超额了，问题也不大，所以我们可以只用业务代码进行一点限制，节省了开发成本
        //todo 然后后续我们就使用其他策略来找出超额的情况进行定制处理
        if(space.getTotalSize()>=space.getMaxCount()||space.getTotalCount()>=space.getMaxCount()){
            log.warn("空间已满");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"空间已满");
        }




        String key = getKey(inputSource,userId,pictureUploadRequest);







        PictureInfoResult pictureInfoResult = UploadPicture(inputSource, key);


        String format = pictureInfoResult.getFormat();
        Integer width = pictureInfoResult.getWidth();
        Integer height = pictureInfoResult.getHeight();
        Double scale = pictureInfoResult.getScale();
        Long size = pictureInfoResult.getSize();
        String url = pictureInfoResult.getUrl();


        //将图片存入数据库
        Picture picture = new Picture();
        picture.setpUrl(pictureInfoResult.getUrl());
        String picName = pictureUploadRequest.getPicName();
        if(ObjUtil.isNotEmpty(picName)){
            picture.setpName(picName);
        }else{
            picture.setpName(pictureInfoResult.getName());
        }
        picture.setpIntroduction("该图片很懒，什么都没留下");
        picture.setpCategory("未分类");
        picture.setpSize(size);
        picture.setpWidth(width);
        picture.setpHeight(height);
        picture.setpScale(scale);
        picture.setpFormat(format);
        picture.setThumbnailUrl(url);
        picture.setUserId(loginUserVO.getId());

        if (loginUserVO.getUserType()== UserConstant.ADMIN){
            fillAuditor(loginUserVO, picture);
        }else if(ObjUtil.isNotEmpty(space)){
            fillAuditor(loginUserVO, picture);
        }else{
            picture.setAuditStatus(AuditStatusEnum.REVIEWING.getCode());
        }
        if(pictureId!=null){
            picture.setId(pictureId);
        }
        //这里有缺陷，如果是不要旧图片，更新成新图片，则需要减去原来的图片大小，然后加上新的图片大小
        boolean update = spaceService.lambdaUpdate()
                .eq(Space::getId, spaceId)
                .setSql("total_size = total_size + " + picture.getpSize())
                .setSql("total_count = total_count + 1")
                .update();
        if(!update){
            log.warn("更新空间额度失败");
            ThrowUtil.throwBusinessException(CodeEnum.SYSTEM_ERROR,"更新空间额度失败");
        }
        return picture;
    }

    private void fillAuditor(UserVO loginUserVO, Picture picture) {
        picture.setAuditorId(loginUserVO.getId());
        picture.setAuditTime(new Date());
        picture.setAuditMsg("审核通过");
        picture.setAuditStatus(AuditStatusEnum.REVIEW_PASS.getCode());
    }


    public abstract void vailPicture(Object inputSource);

    public abstract String getKey(Object inputSource,Long userId,PictureUploadRequest pictureUploadRequest);

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


    public PictureInfoResult getPictureInfoResult(String originalFilename, CIObject compressedCiObject, CIObject thumbnaliPicCiObject) {
        String thumbnailKey = thumbnaliPicCiObject.getKey();
        Integer width = compressedCiObject.getWidth();
        Integer height = compressedCiObject.getHeight();
        long size = compressedCiObject.getSize().longValue();
        Double scale = NumberUtil.round(width * 1.0 / height, 2).doubleValue();
        String format = compressedCiObject.getFormat();
        String key = compressedCiObject.getKey();
        PictureInfoResult pictureInfoResult = new PictureInfoResult();
        pictureInfoResult.setFormat(format);
        pictureInfoResult.setWidth(width);
        pictureInfoResult.setHeight(height);
        pictureInfoResult.setScale(scale);
        pictureInfoResult.setSize(size);
        pictureInfoResult.setUrl(cosService.getHost() + "/" + key);
        pictureInfoResult.setName(FileUtil.getPrefix(originalFilename));
        pictureInfoResult.setThumbnailUrl(cosService.getHost() + "/" + thumbnailKey);
        return pictureInfoResult;
    }








}
