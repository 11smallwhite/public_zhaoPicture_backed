package com.zhao.zhaopicturebacked.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhao.zhaopicturebacked.common.UserConstant;
import com.zhao.zhaopicturebacked.cos.CosService;
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

import com.zhao.zhaopicturebacked.upload.FilePictureUpload;
import com.zhao.zhaopicturebacked.upload.PictureUploadTemplate;
import com.zhao.zhaopicturebacked.utils.ThrowUtil;
import com.zhao.zhaopicturebacked.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.*;

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
    private UserService userService;

    /**
     * 上传图片
     * @param
     * @param pictureId
     * @return
     */
    @Override
    public PictureVO uploadPicture(Object inputSource,Long pictureId,LoginUserVO loginUserVO) {
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
            String key = String.format("/public/%s.%s.%s",oldPicture.getUserId(),oldPicture.getpName(),oldPicture.getpFormat());

            cosService.deletePicture(key);
        }
        PictureUploadTemplate pictureUploadTemplate = null;
        if (inputSource instanceof MultipartFile){
            pictureUploadTemplate = new FilePictureUpload();
        }else if (inputSource instanceof String){
            pictureUploadTemplate = new FilePictureUpload();
        }
        if(pictureUploadTemplate==null){
            log.warn("不支持未知的方式上传图片");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"不支持未知的方式上传图片");
        }
        Picture picture = pictureUploadTemplate.uploadPicture(inputSource, pictureId, loginUserVO);
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
        String key = String.format("/public/%s.%s.%s",userId, picture.getpName(), picture.getpFormat());
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
        if (loginUserVO.getUserType()== UserConstant.ADMIN){
            picture.setAuditorId(loginUserVO.getId());
            picture.setAuditTime(new Date());
            picture.setAuditMsg("审核通过");
            picture.setAuditStatus(AuditStatusEnum.REVIEW_PASS.getCode());
        }else{
            picture.setAuditStatus(AuditStatusEnum.REVIEWING.getCode());
        }
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

}




