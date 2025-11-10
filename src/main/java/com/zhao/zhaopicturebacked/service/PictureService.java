package com.zhao.zhaopicturebacked.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhao.zhaopicturebacked.domain.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhao.zhaopicturebacked.model.LoginUserVO;
import com.zhao.zhaopicturebacked.model.PictureVO;
import com.zhao.zhaopicturebacked.request.picture.PictureEditRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
* @author Vip
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-11-05 09:50:11
*/
public interface PictureService extends IService<Picture> {
    PictureVO uploadPicture(MultipartFile multipartFile,Long pictureId,Long userId);
    Long deletePicture(Long id, LoginUserVO loginUserVO);
    Page<Picture> selectPage(Long id,Long userId, String searchText ,String pCategory, List<String> pTags,Long pSize,Integer pWidth,Integer pHeight,Double pScale, String sortField, String sortOrder, Integer  pageNum, Integer pageSize);
    PictureVO editPicture(PictureEditRequest pictureEditRequest,LoginUserVO loginUserVO);
    PictureVO getPictureVOById(Long id);
}
