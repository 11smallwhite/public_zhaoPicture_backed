package com.zhao.zhaopicturebacked.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhao.zhaopicturebacked.annotation.AuthType;
import com.zhao.zhaopicturebacked.common.BaseResponse;
import com.zhao.zhaopicturebacked.domain.Picture;
import com.zhao.zhaopicturebacked.domain.Space;
import com.zhao.zhaopicturebacked.domain.User;
import com.zhao.zhaopicturebacked.enums.CodeEnum;
import com.zhao.zhaopicturebacked.enums.SpaceLevelEnum;
import com.zhao.zhaopicturebacked.model.LoginUserVO;
import com.zhao.zhaopicturebacked.model.SpaceVO;
import com.zhao.zhaopicturebacked.model.UserVO;
import com.zhao.zhaopicturebacked.request.DeleteRequest;
import com.zhao.zhaopicturebacked.request.space.SpaceAddRequest;
import com.zhao.zhaopicturebacked.request.space.SpaceEditRequest;
import com.zhao.zhaopicturebacked.request.space.SpaceQueryRequest;
import com.zhao.zhaopicturebacked.service.SpaceService;
import com.zhao.zhaopicturebacked.mapper.SpaceMapper;
import com.zhao.zhaopicturebacked.utils.ResultUtil;
import com.zhao.zhaopicturebacked.utils.ThrowUtil;
import com.zhao.zhaopicturebacked.utils.TokenUtil;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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
    private RedissonClient redissonClient;

    @Resource
    private TransactionTemplate transactionTemplate;


    private final ConcurrentHashMap<String, Object> lockHashMap = new ConcurrentHashMap<>();
    @Override
    public Boolean addSpace(SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        UserVO loginUserVO = TokenUtil.getLoginUserVOFromCookie(request);
        String spaceName = spaceAddRequest.getSpaceName();
        int spaceLevel = spaceAddRequest.getSpaceLevel();

        Space space = new Space();
        space.setSpaceName(spaceName);
        space.setSpaceLevel(spaceLevel);
        validSpace(space,true);
        String lockKey = String.format("space:%s", loginUserVO.getId());
        //String lock = String.format("space:%s", loginUserVO.getId()).intern();
        //Object lock = lockHashMap.computeIfAbsent(lockKey, key -> new Object());
        //todo 分布式场景下，由于synchronized无法跨jvm，所以依然会出现并发问题
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if(lock.tryLock(10,-1, TimeUnit.SECONDS)){
                log.info("线程{}获取锁成功",Thread.currentThread());
                //是否允许用户再创建空间
                transactionTemplate.execute(transactionStatus -> {
                    boolean b = checkUserAddSpace(loginUserVO);
                    if(!b){
                        log.warn("用户已创建空间");
                        ThrowUtil.throwBusinessException(CodeEnum.NOT_AUTH,"用户空间数量超过上限");
                    }
                    //填写创建空间的信息
                    fillSpaceLevel(space);
                    space.setUserId(loginUserVO.getId());
                    space.setEditTime(new Date());
                    boolean save = this.save(space);
                    if(!save){
                        log.error("创建空间失败");
                        ThrowUtil.throwBusinessException(CodeEnum.SYSTEM_ERROR,"创建空间失败");
                    }
                    return Optional.ofNullable(save).orElse( false);
                });
            }
        }catch (Exception e){
            log.error("添加空间失败",e);
            ThrowUtil.throwBusinessException(CodeEnum.SYSTEM_ERROR,"添加空间失败");
        }finally {
            if(lock.isHeldByCurrentThread()){
                lock.unlock();
            }
        }

//        synchronized(lock){
//            //是否允许用户再创建空间
//            transactionTemplate.execute(transactionStatus -> {
//                boolean b = checkUserAddSpace(loginUserVO);
//                if(!b){
//                    log.warn("用户已创建空间");
//                    ThrowUtil.throwBusinessException(CodeEnum.NOT_AUTH,"用户空间数量超过上限");
//                }
//                //填写创建空间的信息
//                fillSpaceLevel(space);
//                space.setUserId(loginUserVO.getId());
//                space.setEditTime(new Date());
//                boolean save = this.save(space);
//                if(!save){
//                    log.error("创建空间失败");
//                    ThrowUtil.throwBusinessException(CodeEnum.SYSTEM_ERROR,"创建空间失败");
//                }
//                return Optional.ofNullable(save).orElse( false);
//            });
//
//        }


        return true;
    }

    @Override
    public Boolean editSpace(SpaceEditRequest spaceEditRequest, HttpServletRequest request) {
        Long id = spaceEditRequest.getId();
        String spaceName = spaceEditRequest.getSpaceName();
        int spaceLevel = spaceEditRequest.getSpaceLevel();
        //校验参数
        Space space = new Space();
        space.setSpaceName(spaceName);
        space.setSpaceLevel(spaceLevel);
        validSpace(space,false);
        //校验是不是空间创建者
        UserVO loginUserVO = TokenUtil.getLoginUserVOFromCookie(request);
        Space oldSpace = this.getById(id);
        if(ObjUtil.isEmpty(oldSpace)){
            log.warn("用户没有权限修改空间");
            ThrowUtil.throwBusinessException(CodeEnum.NOT_AUTH,"用户没有权限修改空间");
        }
        space.setId(id);


        //空间等级决定空间大小，只能从小变大
        if(spaceLevel>oldSpace.getSpaceLevel()){
            fillSpaceLevel(space);
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

    @Override
    public Long deleteSpaceById(DeleteRequest deleteRequest, HttpServletRequest request) {
        Long id = deleteRequest.getId();
        UserVO loginUserVO = TokenUtil.getLoginUserVOFromCookie(request);
        //1.校验id
        if (ObjUtil.isEmpty(id)){
            log.warn("id参数为空");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"id参数为空");
        }
        //2.查询被删除的数据是否存在
        Space space = this.getById(id);
        if (ObjUtil.isEmpty(space)){
            log.warn("id对应的数据不存在");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"id对应数据不存在");
        }
        Long userId = space.getUserId();
        Integer userType = loginUserVO.getUserType();
        Long loginUserVOId = loginUserVO.getId();
        //3.校验用户有无权限删除这张图片
        if(!userId.equals(loginUserVOId)&&userType!=1){
            log.warn("用户没有权限删除这个空间");
            ThrowUtil.throwBusinessException(CodeEnum.NOT_AUTH,"无权限");
        }
        boolean b = this.removeById(id);
        if (!b){
            log.warn("删除空间失败");
            ThrowUtil.throwBusinessException(CodeEnum.SYSTEM_ERROR,"删除空间失败");
        }
        return id;
    }

    @Override
    public Page<Space> selectSpace(SpaceQueryRequest spaceQueryRequest) {

        QueryWrapper<Space> spaceQueryWrapper = getSpaceQueryWrapper(spaceQueryRequest);

        Integer pageNum = spaceQueryRequest.getPageNum();
        Integer pageSize = spaceQueryRequest.getPageSize();
        Page<Space> spacePage = this.page(new Page<>(pageNum, pageSize), spaceQueryWrapper);

        return spacePage;
    }

    private QueryWrapper<Space> getSpaceQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        Long id = spaceQueryRequest.getId();
        String spaceName = spaceQueryRequest.getSpaceName();
        int spaceLevel = spaceQueryRequest.getSpaceLevel();
        Long userId = spaceQueryRequest.getUserId();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();


        QueryWrapper<Space> spaceQueryWrapper = new QueryWrapper<>();
        spaceQueryWrapper.eq(ObjUtil.isNotEmpty( id),"id",id);
        spaceQueryWrapper.eq(ObjUtil.isNotEmpty( spaceName),"spaceName",spaceName);
        spaceQueryWrapper.eq(ObjUtil.isNotEmpty( spaceLevel),"spaceLevel",spaceLevel);
        spaceQueryWrapper.eq(ObjUtil.isNotEmpty( userId),"userId",userId);
        String s = convertFieldToColumn(sortField);
        spaceQueryWrapper.orderBy(ObjUtil.isNotNull(s), sortOrder.equals("asc"), s);
        return spaceQueryWrapper;
    }

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
            case "spaceName":
                return "space_name";
            case "spaceLevel":
                return "space_level";
            case "maxSize":
                return "max_size";
            case "maxCount":
                return "max_count";
            case "userId":
                return "user_id";
            case "totalSize":
                return "total_size";
            case "totalCount":
                return "total_count";
            default:
                return fieldName; // 如果已经是数据库字段名，直接返回
        }
    }


    public void fillSpaceLevel(Space space) {
        SpaceLevelEnum spaceLevelEnumByCode = SpaceLevelEnum.getSpaceLevelEnumByCode(space.getSpaceLevel());
        if(spaceLevelEnumByCode!=null){
            long maxCount = spaceLevelEnumByCode.getMaxCount();
            if(space.getMaxCount()==null){
                space.setMaxCount(maxCount);
            }
            long maxSize = spaceLevelEnumByCode.getMaxSize();
            if(space.getMaxSize()==null){
                space.setMaxSize(maxSize);
            }
        }
    }

    @Override
    public Space spaceLevelelCheck(Space space, long count, long size) {
        Long totalCount = space.getTotalCount();
        Long totalSize = space.getTotalSize();
        if(totalCount+count>space.getMaxCount()){
            log.warn("用户空间数量超过上限");
            ThrowUtil.throwBusinessException(CodeEnum.NOT_AUTH,"用户空间数量超过上限");
        }
        if(totalSize+size>space.getMaxSize()){
            log.warn("用户空间容量超过上限");
            ThrowUtil.throwBusinessException(CodeEnum.NOT_AUTH,"用户空间容量超过上限");
        }
        space.setTotalCount(totalCount+count);
        space.setTotalSize(totalSize+size);
        return space;
    }


    @Override
    public void validSpace(Space space, boolean add) {
        if(space ==null){
            log.warn("空间为空");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"空间为空");
        }
        // 从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getSpaceLevelEnumByCode(spaceLevel);
        // 要创建
        if (add) {
            if (StrUtil.isBlank(spaceName)) {
                ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"空间名称不能为空");
            }
            if (spaceLevel == null) {
                ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"空间级别不能为空");
            }
        }
        // 修改数据时，如果要改空间级别
        if (spaceLevel != null && spaceLevelEnum == null) {
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"空间级别错误");
        }
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"空间名称过长");
        }
    }

    @Override
    public void validSpaceAndPicture(Space space, Picture picture) {
        Long spaceId = picture.getSpaceId();
        Long id = space.getId();
        if (spaceId == null||id==null) {
            log.error("空间id为空");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"空间id为空");
        }
        if (!spaceId.equals(id)) {
            log.error("空间id不一致");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"空间id不一致");
        }
    }

    @Override
    public void validSpaceAndUserVO(Space space, UserVO userVO) {
        User user = new User();
        BeanUtils.copyProperties(userVO,user);
        validSpaceAndUser(space,user);
    }

    @Override
    public void validSpaceAndUser(Space space, User user) {
        Long userId = space.getUserId();
        Long id = user.getId();
        if (userId == null||id==null) {
            log.error("用户id为空");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"用户id为空");
        }
        if (!userId.equals(id)) {
            log.error("用户id不一致");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"用户id不一致");
        }
    }

    @Override
    public void validUserAndPicture(User user, Picture picture) {
        Long id = user.getId();
        Long userId = picture.getUserId();
        if (userId == null||id==null) {
            log.error("用户id为空");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"用户id为空");
        }
        if (!userId.equals(id)&&user.getUserType()!=1) {
            log.error("用户对这张图片无权限");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"用户对这张图片无权限");
        }
    }

    @Override
    public void validUserVOAndPicture(UserVO userVO, Picture picture) {
        User user = new User();
        BeanUtils.copyProperties(userVO,user);
        validUserAndPicture(user,picture);
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




