package com.zhao.zhaopicturebacked.service;

import com.zhao.zhaopicturebacked.domain.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhao.zhaopicturebacked.model.SpaceVO;
import com.zhao.zhaopicturebacked.request.space.SpaceAddRequest;
import com.zhao.zhaopicturebacked.request.space.SpaceEditRequest;

import javax.servlet.http.HttpServletRequest;

/**
* @author Vip
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-11-23 15:05:36
*/
public interface SpaceService extends IService<Space> {

    Boolean addSpace(SpaceAddRequest spaceAddRequest, HttpServletRequest  request);
    Boolean editSpace(SpaceEditRequest spaceEditRequest, HttpServletRequest request);
}
