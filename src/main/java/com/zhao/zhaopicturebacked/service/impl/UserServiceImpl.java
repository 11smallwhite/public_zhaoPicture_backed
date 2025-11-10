package com.zhao.zhaopicturebacked.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhao.zhaopicturebacked.domain.User;
import com.zhao.zhaopicturebacked.enums.CodeEnum;
import com.zhao.zhaopicturebacked.model.UserVO;
import com.zhao.zhaopicturebacked.service.UserService;
import com.zhao.zhaopicturebacked.mapper.UserMapper;
import com.zhao.zhaopicturebacked.utils.ThrowUtil;
import com.zhao.zhaopicturebacked.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
* @author Vip
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-10-30 13:13:51
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{


    /**
     * 用户注册
     * @param account
     * @param password
     */
    @Override
    public Long userRegister(String account, String password, String checkPassword) {
        //1.校验参数是否为空
        if (StrUtil.isAllBlank(account,password,checkPassword)){
            log.info("账号或密码为空");
            ThrowUtil.throwBusinessException(CodeEnum.NULL,"有参数为空");
        }
        //2.校验密码是否>=6,<=16，账号长度是否为10位
        if (password.length()<6 || password.length()>16|| account.length()!=10){
            log.info("账号长度或密码长度错误");
            ThrowUtil.throwBusinessException(CodeEnum.OUT_OF_RANGE,"账号和密码的长度范围错误");
        }
        //3.校验密码是否相等
        if (!password.equals(checkPassword)){
            log.info("密码不一致");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"密码不一致");
        }
        //4.校验账号是否存在
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("user_account",account);
        if (this.count(userQueryWrapper)>0){
            log.info("账号已存在");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"账号已存在");
        }
        //5.加密密码
        String encryptPassword = SecureUtil.md5( password);
        //6.插入数据
        User user = new User();
        user.setUserAccount(account);
        user.setUserPassword(encryptPassword);
        user = UserUtil.fillUser(user);
        boolean save = this.save(user);
        if (!save){
            log.info("数据库插入数据失败");
            ThrowUtil.throwBusinessException(CodeEnum.SYSTEM_ERROR,"数据库插入数据失败");
        }
        return user.getId();
    }

    @Override
    public User userLogin(String account, String password) {
        //1.校验参数是否为空
        if (StrUtil.isAllBlank(account,password)){
            log.info("有参数为空");
            ThrowUtil.throwBusinessException(CodeEnum.NULL,"有参数为空");
        }
        //2.校验账号是否存在
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("user_account",account);
        User dataSourceUser = this.getOne(userQueryWrapper);
        if (ObjUtil.isEmpty(dataSourceUser)){
            log.info("账号不存在");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"账号不存在");
        }
        //3.校验密码是否正确
        String encryptPassword = SecureUtil.md5( password);
        if (!encryptPassword.equals(dataSourceUser.getUserPassword())){
            log.info("密码错误");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"密码错误");
        }
        //4.返回成功登录的用户数据
        return dataSourceUser;
    }


    @Override
    public UserVO userEdit(Long id,String userName, String userIntroduction, String userAvatar, User loginUser) {
        //1.校验id是否为空
        if (ObjUtil.isEmpty(id)){
            log.info("id为空，无法更新");
            ThrowUtil.throwBusinessException(CodeEnum.NULL,"id为空");
        }

        //2.校验id是否是当前用户
        if (!id.equals(loginUser.getId())){
            log.info("id不是当前用户，无法更新");
            ThrowUtil.throwBusinessException(CodeEnum.NOT_AUTH,"id不是当前用户");
        }

        //3.更新值
        User user = new User();
        user.setId(id);
        user.setUserName(userName);
        user.setUserIntroduction(userIntroduction);
        user.setUserAvatar(userAvatar);
        user.setUserType(loginUser.getUserType());
        user.setEditTime(new Date());
        user = UserUtil.fillUser(user);
        boolean update = this.updateById(user);
        if (!update){
            log.warn("用户信息更新失败");
            ThrowUtil.throwBusinessException(CodeEnum.SYSTEM_ERROR,"用户信息更新失败");
        }
        UserVO userVO = UserUtil.getUserVOByUser(user);
        return userVO;
    }

    @Override
    public List<User> select(Long id, String userAccount, String userName, String userIntrodution,String sortField,String sortOrder) {
        //1.构造查询条件
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        if (id>0){
            userQueryWrapper.eq("id",id);
        }
        userQueryWrapper.eq(ObjUtil.isNotEmpty(userAccount),"user_account",userAccount);
        userQueryWrapper.like("user_name",userName);
        userQueryWrapper.like("user_introduction",userIntrodution);
        userQueryWrapper.orderBy(ObjUtil.isNotEmpty(sortField),sortOrder.equals("asc"),sortField);
        //2.根据查询条件进行list查询
        List<User> userList = this.list(userQueryWrapper);
        if (ObjUtil.isEmpty(userList)){
            log.info("没有找到符合条件的用户");
            ThrowUtil.throwBusinessException(CodeEnum.NOT_FOUND,"没有找到符合条件的用户");
        }

        return userList;
    }

    @Override
    public Page<User> selectPage(Long id, String userAccount, String userName, String userIntrodution,String sortField,String sortOrder, Integer pageNum, Integer pageSize) {

        //1.构造查询条件
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq(ObjUtil.isNotNull(id),"id",id);
        userQueryWrapper.eq(ObjUtil.isNotEmpty(userAccount),"user_account",userAccount);
        userQueryWrapper.like(ObjUtil.isNotNull(userName),"user_name",userName);
        userQueryWrapper.like(ObjUtil.isNotNull(userIntrodution),"user_introduction",userIntrodution);
        if (ObjUtil.isNull(sortOrder)){
            sortOrder = "asc";
        }
        String s = convertFieldToColumn(sortField);
        userQueryWrapper.orderBy(ObjUtil.isNotEmpty(sortField),sortOrder.equals("asc"),s);
        //2.根据查询条件进行page查询
        Page<User> userPage = this.page(new Page<>(pageNum, pageSize), userQueryWrapper);
        if (ObjUtil.isEmpty(userPage)){
            log.info("没有找到符合条件的用户");
            ThrowUtil.throwBusinessException(CodeEnum.NOT_FOUND,"没有找到符合条件的用户");
        }
        return userPage;
    }

    @Override
    public Long userDelete(Long id) {
        //1.校验id是否为空
        if (ObjUtil.isEmpty(id)||id<=0){
            log.info("id错误，无法删除");
            ThrowUtil.throwBusinessException(CodeEnum.NULL,"id错误");
        }
        //2.删除用户
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        boolean delete = this.removeById(id);
        if (!delete){
            log.info("删除用户失败,id不存在");
            ThrowUtil.throwBusinessException(CodeEnum.SYSTEM_ERROR,"删除用户失败,id不存在");
        }
        return id;


    }

    // 字段名映射方法
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
            case "userName":
                return "user_name";
            case "userAccount":
                return "user_account";
            case "userIntroduction":
                return "user_introduction";
            case "userAvatar":
                return "user_avatar";
            case "userType":
                return "user_type";
            case "isDeleted":
                return "is_deleted";
            case "id":
                return "id";
            default:
                return fieldName; // 如果已经是数据库字段名，直接返回
        }
    }
}