package com.zhao.zhaopicturebacked.controller;


import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhao.zhaopicturebacked.annotation.AuthType;
import com.zhao.zhaopicturebacked.common.BaseResponse;
import com.zhao.zhaopicturebacked.common.UserConstant;
import com.zhao.zhaopicturebacked.domain.User;
import com.zhao.zhaopicturebacked.enums.CodeEnum;
import com.zhao.zhaopicturebacked.model.LoginUserVO;
import com.zhao.zhaopicturebacked.model.UserVO;
import com.zhao.zhaopicturebacked.request.*;
import com.zhao.zhaopicturebacked.request.user.UserEditRequest;
import com.zhao.zhaopicturebacked.request.user.UserLoginRequest;
import com.zhao.zhaopicturebacked.request.user.UserQueryRequest;
import com.zhao.zhaopicturebacked.request.user.UserRegisterRequest;
import com.zhao.zhaopicturebacked.service.UserService;
import com.zhao.zhaopicturebacked.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user")
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    @Resource
    private UserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;



    /**
     * 用户注册
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest){
        String account = userRegisterRequest.getUserAccount();
        String password = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        Long id = userService.userRegister(account, password, checkPassword);
        log.info("用户注册成功：{}", id);
        return ResultUtil.success(id,"注册成功");
    }


    /**
     * 用户登录
     * @param userLoginRequest
     * @param request
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request, HttpServletResponse response){
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        User user = userService.userLogin(userAccount, userPassword);
        LoginUserVO loginUserVO = UserUtil.getLoginUserVOByUser(user);
        //1.获取token
        String token = TokenUtil.getToken(user.getId(), user.getUserAccount(), user.getUserName());
        //1.1删除旧的token
        stringRedisTemplate.opsForValue().getAndDelete(token);
        //2.将生成的token作为key，用户信息作为value，
        try {
            stringRedisTemplate.opsForValue().set(token, JSONUtil.toJsonStr(loginUserVO),60*60, TimeUnit.SECONDS);
        }catch (Exception e){
            log.warn("存储token到redis里失败了");
            ThrowUtil.throwBusinessException(CodeEnum.NOT_AUTH,"存储token到redis里失败了");
        }
        //3.将token用Cookie形式返回给前端
        TokenUtil.setTokenToCookie(token, response);

        log.info("用户登录成功：{}", loginUserVO);
        return ResultUtil.success(loginUserVO,"登录成功");
    }

    @GetMapping("/get")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request,HttpServletResponse response){
        //1.从请求的Cookie里拿到token
        String token = TokenUtil.getTokenFromCookie(request);
        if (ObjUtil.isEmpty(token)){
            log.info("从请求的Cookie里没有拿到token或者token为null");
            ThrowUtil.throwBusinessException(CodeEnum.NOT_AUTH,"从请求的Cookie里没有拿到token或者token为null");
        }
        //2.从redis里拿到用户信息
        String loginUserVOJson = stringRedisTemplate.opsForValue().get(token);
        if (ObjUtil.isEmpty(loginUserVOJson)){
            log.info("从redis里没有找到用户信息,token已过期或未登录");
            ThrowUtil.throwBusinessException(CodeEnum.NOT_AUTH,"loginUser为空");
        }
        //续期redis
        stringRedisTemplate.expire(token,60*60, TimeUnit.SECONDS);
        //续期cookie
        TokenUtil.setTokenToCookie(token, response);
        LoginUserVO loginUserVO = JSONUtil.toBean(loginUserVOJson, LoginUserVO.class);

        return ResultUtil.success(loginUserVO,"获取用户信息成功");

    }


    /**
     * 用户退出
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request,HttpServletResponse response){
        String token = TokenUtil.getTokenFromCookie(request);
        //删除token
        stringRedisTemplate.opsForValue().getAndDelete(token);
        //删除Cookie
        TokenUtil.setTokenToCookie(null, response);
        return ResultUtil.success(true,"退出成功");
    }

    /**
     * 用户编辑
     * @param userEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    @AuthType(userType = 0)
    public BaseResponse<UserVO> userEdit(@RequestBody UserEditRequest userEditRequest, HttpServletRequest request){
        //1.校验参数
        Long id = userEditRequest.getId();
        String userName = userEditRequest.getUserName();
        String userIntroduction = userEditRequest.getUserIntroduction();
        String userAvatar = userEditRequest.getUserAvatar();
        //2.获取当前登录用户
        String token = TokenUtil.getTokenFromCookie(request);
        String loginUserVOJson = stringRedisTemplate.opsForValue().get(token);
        LoginUserVO loginUserVO = JSONUtil.toBean(loginUserVOJson, LoginUserVO.class);
        User loginUser = UserUtil.getUserByLoginUserVO(loginUserVO);
        //3.调用service层方法
        UserVO userVO = userService.userEdit(id, userName, userIntroduction, userAvatar, loginUser);
        return ResultUtil.success(userVO,"编辑成功");
    }

    /**
     * 分页查询用户----用户可用
     * @param userQueryRequest
     * @return
     */
    @PostMapping("/page/select/query")
    @AuthType(userType = 0)
    public BaseResponse<Page<UserVO>> select(@RequestBody UserQueryRequest userQueryRequest){
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userIntroduction = userQueryRequest.getUserIntroduction();
        String userName = userQueryRequest.getUserName();
        Integer pageNum = userQueryRequest.getPageNum();
        Integer pageSize = userQueryRequest.getPageSize();
        String sortField = userQueryRequest.getSortField();

        String sortOrder = userQueryRequest.getSortOrder();
       
        //想要得到LoginUserVO的分页数据，就得先得到User的分页数据,也就是Page<User>,
        // 不能说 先得到List<User>，然后将其转换为List<LoginUserVO>,再转换为Page<LoginUserVO>，这样得到的Page<LoginUserVO>其实就是只有一页数据的Page，
        // 因为你new的Page对象，是没有进行过分页的，没有分页参数的
        // 如果你先通过分页查询得到Page<User>，那么你就可以这样子new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal())，这样才是真正的分页.
        Page<User> userPage = userService.selectPage(id, userAccount,  userName, userIntroduction, sortField, sortOrder, pageNum, pageSize);
        List<UserVO> userVOList = userPage.getRecords().stream().map(user -> UserUtil.getUserVOByUser(user)).collect(Collectors.toList());
        // 构造返回的分页对象
        Page<UserVO> userVOPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        userVOPage.setRecords(userVOList);
        return ResultUtil.success(userVOPage,"查询成功");
    }

    /**
     * 分页查询用户----管理员专用(废案，因为用户的密码会在网络间传输，不安全)
     * @param
     * @return
     */
//    @PostMapping("/page/select/query/admin")
//    @AuthType(userType = 1)
//    public BaseResponse<Page<User>> adminSelect(@RequestBody UserQueryRequest userQueryRequest){
//        Long id = userQueryRequest.getId();
//        String userAccount = userQueryRequest.getUserAccount();
//        String userName = userQueryRequest.getUserName();
//        String userIntroduction = userQueryRequest.getUserIntroduction();
//        Integer pageNum = userQueryRequest.getPageNum();
//        Integer pageSize = userQueryRequest.getPageSize();
//        String sortField = userQueryRequest.getSortField();
//
//        String sortOrder = userQueryRequest.getSortOrder();
//
//        //想要得到LoginUserVO的分页数据，就得先得到User的分页数据,也就是Page<User>,
//        // 不能说 先得到List<User>，然后将其转换为List<LoginUserVO>,再转换为Page<LoginUserVO>，这样得到的Page<LoginUserVO>其实就是只有一页数据的Page，
//        // 因为你new的Page对象，是没有进行过分页的，没有分页参数的
//        // 如果你先通过分页查询得到Page<User>，那么你就可以这样子new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal())，这样才是真正的分页.
//        Page<User> userPage = userService.selectPage(id, userAccount, userName, userIntroduction, sortField, sortOrder, pageNum, pageSize);
//
//        return ResultUtil.success(userPage,"查询成功");
//    }


    @PostMapping("/delete")
    @AuthType(userType = 1)
    public BaseResponse<Long> userDeleteById(@RequestBody DeleteRequest deleteRequest){
        Long id = deleteRequest.getId();
        Long delete = userService.userDelete(id);
        return ResultUtil.success(delete,"删除成功");
    }

}