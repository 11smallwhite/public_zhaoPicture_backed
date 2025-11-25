package com.zhao.zhaopicturebacked.service;

import com.zhao.zhaopicturebacked.domain.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhao.zhaopicturebacked.enums.SpaceLevelEnum;
import com.zhao.zhaopicturebacked.model.SpaceVO;
import com.zhao.zhaopicturebacked.request.DeleteRequest;
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
    Long deleteSpaceById(DeleteRequest deleteRequest, HttpServletRequest request);
    //给空间填充Level参数
    void fillSpaceLevel(Space space);
    //给空间更新容量
    Space spaceLevelelCheck(Space space,long count,long size);
    //校验空间的参数
    void validSpace(Space space,boolean add);
}
