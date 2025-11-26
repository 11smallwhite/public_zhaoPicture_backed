package com.zhao.zhaopicturebacked.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhao.zhaopicturebacked.domain.Picture;
import com.zhao.zhaopicturebacked.model.LoginUserVO;
import com.zhao.zhaopicturebacked.model.PictureVO;
import com.zhao.zhaopicturebacked.model.UserVO;
import com.zhao.zhaopicturebacked.request.DeleteRequest;
import com.zhao.zhaopicturebacked.request.picture.*;
import org.springframework.web.multipart.MultipartFile;

/**
* @author Vip
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-11-05 09:50:11
*/
public interface PictureService extends IService<Picture> {
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, UserVO loginUserVO);
    int uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, UserVO loginUserVO);
    Long deletePicture(DeleteRequest deleteRequest, UserVO loginUserVO);
    Page<Picture> selectPage(PictureQueryRequest pictureQueryRequest);
    PictureVO editPicture(PictureEditRequest pictureEditRequest,UserVO loginUserVO);
    PictureVO getPictureVOById(Long id);
    void auditPicture(PictureAuditRequest pictureAuditRequest, LoginUserVO loginUserVO);
}
