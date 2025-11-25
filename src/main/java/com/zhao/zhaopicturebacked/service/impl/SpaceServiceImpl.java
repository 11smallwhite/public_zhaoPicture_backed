package com.zhao.zhaopicturebacked.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhao.zhaopicturebacked.domain.Space;
import com.zhao.zhaopicturebacked.enums.CodeEnum;
import com.zhao.zhaopicturebacked.enums.SpaceLevelEnum;
import com.zhao.zhaopicturebacked.model.SpaceVO;
import com.zhao.zhaopicturebacked.model.UserVO;
import com.zhao.zhaopicturebacked.request.space.SpaceAddRequest;
import com.zhao.zhaopicturebacked.request.space.SpaceEditRequest;
import com.zhao.zhaopicturebacked.service.SpaceService;
import com.zhao.zhaopicturebacked.mapper.SpaceMapper;
import com.zhao.zhaopicturebacked.utils.ThrowUtil;
import com.zhao.zhaopicturebacked.utils.TokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
* @author Vip
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2025-11-23 15:05:36
*/
@Service
@Slf4j
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceService{


    @Resource
    private StringRedisTemplate stringRedisTemplate;



    @Override
    public Boolean addSpace(SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        //先从redis拿到登录用户的VO信息
        String tokenFromCookie = TokenUtil.getTokenFromCookie(request);
        String loginUserVOJson = stringRedisTemplate.opsForValue().get(tokenFromCookie);
        UserVO loginUserVO = JSONUtil.toBean(loginUserVOJson, UserVO.class);
        //然后根据前端传来的spaceLevel，找到对应的枚举类
        String spaceName = spaceAddRequest.getSpaceName();
        int spaceLevel = spaceAddRequest.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getSpaceLevelEnumByCode(spaceLevel);
        if(spaceLevelEnum == null||StrUtil.isBlank(spaceName)){
            log.warn("前端传来的参数错误");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"前端传来的参数错误");
        }
        //是否允许用户再创建空间
        boolean b = checkUserAddSpace(loginUserVO);
        if(!b){
            log.warn("用户已创建空间");
            ThrowUtil.throwBusinessException(CodeEnum.NOT_AUTH,"用户空间数量超过上限");
        }
        //填写创建空间的信息
        Space space = new Space();
        space.setSpaceName(spaceName);
        fillSpaceLevel(space, spaceLevelEnum);
        space.setUserId(loginUserVO.getId());
        space.setEditTime(new Date());
        boolean save = this.save(space);
        if(!save){
             log.error("创建空间失败");
             ThrowUtil.throwBusinessException(CodeEnum.SYSTEM_ERROR,"创建空间失败");
        }
        return true;
    }

    @Override
    public Boolean editSpace(SpaceEditRequest spaceEditRequest, HttpServletRequest request) {
        Long id = spaceEditRequest.getId();
        String spaceName = spaceEditRequest.getSpaceName();
        int spaceLevel = spaceEditRequest.getSpaceLevel();
        //校验参数是否为空
        if (ObjUtil.hasNull(id,spaceLevel)){
            log.warn("参数为空");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"参数为空");
        }
        //校验是不是图片创建者
        String tokenFromCookie = TokenUtil.getTokenFromCookie(request);
        String loginUserVOJson = stringRedisTemplate.opsForValue().get(tokenFromCookie);
        UserVO loginUserVO = JSONUtil.toBean(loginUserVOJson, UserVO.class);
        Space oldSpace = this.getById(id);
        if(ObjUtil.isEmpty(oldSpace)){
            log.warn("用户没有权限修改空间");
            ThrowUtil.throwBusinessException(CodeEnum.NOT_AUTH,"用户没有权限修改空间");
        }
        Space space = new Space();
        space.setId(id);
        if(StrUtil.isNotEmpty(spaceName)){
            space.setSpaceName(spaceName);
        }
        SpaceLevelEnum oldSpaceLevelEnum = SpaceLevelEnum.getSpaceLevelEnumByCode(oldSpace.getSpaceLevel());
        SpaceLevelEnum newSpaceLevelEnum = SpaceLevelEnum.getSpaceLevelEnumByCode(spaceLevel);
        //如果更新的空间等级和旧的空间等级不一致，则需要更新空间等级和空间容量
        if(!oldSpaceLevelEnum.equals(newSpaceLevelEnum)){
            fillSpaceLevel(space, newSpaceLevelEnum);
        }
        space.setEditTime(new Date());
        space.setUserId(loginUserVO.getId());

        boolean update = this.updateById(space);
        if(!update){
            log.error("更新空间失败");
            ThrowUtil.throwBusinessException(CodeEnum.SYSTEM_ERROR,"更新空间失败");
        }
        return true;
    }

    private void fillSpaceLevel(Space space, SpaceLevelEnum newSpaceLevelEnum) {
        space.setSpaceLevel(newSpaceLevelEnum.getCode());
        space.setMaxSize(newSpaceLevelEnum.getMaxSize());
        space.setMaxCount(newSpaceLevelEnum.getMaxCount());
    }

    public boolean checkUserAddSpace(UserVO userVO){
        Long userId = userVO.getId();
        //查询Space表，用户创建了几个空间
        Long count = this.lambdaQuery().eq(Space::getUserId, userId).count();
        //如果用户创建的空间数量大于0，则返回false
        //现阶段只允许用户创建1个空间
        if(count>0){
            return false;
        }
        return true;
    }
    

}




