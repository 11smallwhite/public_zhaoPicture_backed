package com.zhao.zhaopicturebacked.utils;

import ch.qos.logback.core.joran.util.beans.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhao.zhaopicturebacked.domain.User;
import com.zhao.zhaopicturebacked.enums.CodeEnum;
import com.zhao.zhaopicturebacked.enums.UserTypeEnum;
import com.zhao.zhaopicturebacked.exception.BusinessException;
import com.zhao.zhaopicturebacked.model.LoginUserVO;
import com.zhao.zhaopicturebacked.model.UserVO;
import com.zhao.zhaopicturebacked.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
public class UserUtil {


    public static User fillUser(User user){
        if (ObjUtil.isEmpty(user.getUserName())){
            user.setUserName("未命名");
        }
        if (ObjUtil.isEmpty(user.getUserIntroduction())){
            user.setUserIntroduction("该用户很懒，没有留下任何简介");
        }
        if (ObjUtil.isEmpty(user.getUserType())){
            user.setUserType(UserTypeEnum.USER.getType());
        }

        return user;
    }



    public static UserVO getUserVOByUser(User user){
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user,userVO);
        if (ObjUtil.isEmpty(userVO)){
            log.info("User转换为UserVO失败");
            ThrowUtil.throwBusinessException(CodeEnum.SYSTEM_ERROR,"User转换为UserVO失败");
        }
        return userVO;
    }


    public static User getUserByLoginUserVO(UserVO loginUserVO){
        User user = new User();
        BeanUtils.copyProperties(loginUserVO,user);
        if (ObjUtil.isEmpty(user)){
            log.info("LoginUserVO转换为LoginUserVO失败");
            ThrowUtil.throwBusinessException(CodeEnum.SYSTEM_ERROR,"LoginUserVO转换为User失败");
        }
        return user;
    }

}
