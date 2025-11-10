package com.zhao.zhaopicturebacked.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhao.zhaopicturebacked.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhao.zhaopicturebacked.model.UserVO;

import java.util.List;

/**
* @author Vip
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-10-30 13:13:51
*/
public interface UserService extends IService<User> {

    //用户注册
    Long userRegister(String account, String password,String checkPassword);
    //用户登录
    User userLogin(String account, String password);
    //用户编辑
    UserVO userEdit(Long id, String userName, String userIntroduction, String userAvatar, User LoginUser);
    //查询用户
    List<User> select(Long id, String userAccount, String userName, String userIntrodution,String sortField,String sortOrder);
    //分页查询用户
    Page<User> selectPage(Long id, String userAccount, String userName,String userIntroduction,String sortField,String sortOrder, Integer pageNum, Integer pageSize);
    //删除用户
    Long userDelete(Long id);

}