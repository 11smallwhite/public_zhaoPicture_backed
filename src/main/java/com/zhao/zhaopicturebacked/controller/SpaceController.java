package com.zhao.zhaopicturebacked.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhao.zhaopicturebacked.annotation.AuthType;
import com.zhao.zhaopicturebacked.common.BaseResponse;
import com.zhao.zhaopicturebacked.common.UserConstant;
import com.zhao.zhaopicturebacked.domain.Space;
import com.zhao.zhaopicturebacked.enums.CodeEnum;
import com.zhao.zhaopicturebacked.model.SpaceVO;
import com.zhao.zhaopicturebacked.request.DeleteRequest;
import com.zhao.zhaopicturebacked.request.space.SpaceAddRequest;
import com.zhao.zhaopicturebacked.request.space.SpaceEditRequest;
import com.zhao.zhaopicturebacked.request.space.SpaceQueryRequest;
import com.zhao.zhaopicturebacked.service.SpaceService;
import com.zhao.zhaopicturebacked.utils.ResultUtil;
import com.zhao.zhaopicturebacked.utils.ThrowUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RequestMapping("/space")
@ResponseBody
@Controller
@Slf4j
public class SpaceController {

    @Resource
    private SpaceService spaceService;


    @AuthType(userType = UserConstant.USER)
    @PostMapping("/add")
    public BaseResponse<Boolean> addSpace(@RequestBody SpaceAddRequest spaceAddRequest, HttpServletRequest  request){

        Boolean b = spaceService.addSpace(spaceAddRequest, request);
        return ResultUtil.success(b);
    }

    @AuthType(userType = UserConstant.USER)
    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditRequest spaceEditRequest, HttpServletRequest  request){
        //校验前端传的参数是否为空
        if (spaceEditRequest==null){
            log.warn("前端传来的参数为空");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"参数为空");
        }
        Boolean b = spaceService.editSpace(spaceEditRequest, request);
        return ResultUtil.success(b);
    }
    @AuthType(userType = UserConstant.USER)
    @PostMapping("/delete")
    public BaseResponse<Long> deleteSpace(@RequestBody DeleteRequest deleteRequest,HttpServletRequest request){
        if(deleteRequest==null){
            log.warn("前端传来的参数为空");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"参数为空");
        }
        Long l = spaceService.deleteSpaceById(deleteRequest, request);
        return ResultUtil.success(l);
    }

    @AuthType(userType = UserConstant.ADMIN)
    @PostMapping("/select/admin")
    public BaseResponse<Page<Space>> selectSpace(@RequestBody SpaceQueryRequest spaceQueryRequest){
        Page<Space> spacePage = spaceService.selectSpace(spaceQueryRequest);

        return ResultUtil.success(null);
    }

}
