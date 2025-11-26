package com.zhao.zhaopicturebacked.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhao.zhaopicturebacked.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhao.zhaopicturebacked.model.UserVO;
import com.zhao.zhaopicturebacked.request.DeleteRequest;
import com.zhao.zhaopicturebacked.request.user.UserEditRequest;
import com.zhao.zhaopicturebacked.request.user.UserLoginRequest;
import com.zhao.zhaopicturebacked.request.user.UserQueryRequest;
import com.zhao.zhaopicturebacked.request.user.UserRegisterRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Vip
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-10-30 13:13:51
*/
public interface UserService extends IService<User> {

    //用户注册
    Long userRegister(UserRegisterRequest userRegisterRequest);
    //用户登录
    User userLogin(UserLoginRequest userLoginRequest);
    //用户编辑
    UserVO userEdit(UserEditRequest userEditRequest, HttpServletRequest request);
    //查询用户
    List<User> select(UserQueryRequest userQueryRequest);
    //分页查询用户
    Page<User> selectPage(UserQueryRequest userQueryRequest);
    //删除用户
    Long userDelete(DeleteRequest deleteRequest);

}