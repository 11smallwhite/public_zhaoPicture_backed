package com.zhao.zhaopicturebacked.utils;

import cn.hutool.core.util.ObjUtil;

import cn.hutool.json.JSONUtil;
import com.zhao.zhaopicturebacked.enums.CodeEnum;
import com.zhao.zhaopicturebacked.model.LoginUserVO;
import com.zhao.zhaopicturebacked.model.UserVO;
import com.zhao.zhaopicturebacked.task.SpringContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Around;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
@Slf4j
public class TokenUtil {

    //1.得到token
    public static String getToken(Long id, String userAccount ,String userName){
        return String.format(UUID.randomUUID() + id.toString() + userAccount + userName);
    }

//    //2.将生成的token作为key，用户信息作为value，存入redis
//
//    public static void setTokenToRedis(String token, LoginUserVO loginUserVO){
//
//        try {
//            stringRedisTemplate.opsForValue().set(token, JSONUtil.toJsonStr(loginUserVO),30, TimeUnit.SECONDS);
//        }catch (Exception e){
//            log.warn("存储token到redis里失败了");
//            ThrowUtil.throwBusinessException(CodeEnum.NOT_AUTH,"存储token到redis里失败了");
//        }
//    }

    //3.将token用Cookie形式返回给前端
    public static void setTokenToCookie(String token, HttpServletResponse response){
        Cookie cookie = new Cookie("token", token);
        // --- 关键Cookie配置 ---
        cookie.setHttpOnly(true);    // 防止XSS攻击，JavaScript无法读取此Cookie
        cookie.setSecure(false);     // 为true时，仅在HTTPS下传输。本地开发用false，生产环境应为true
        cookie.setPath("/");         // Cookie的有效路径，'/'表示全站有效
        cookie.setMaxAge(60*60);   // Cookie过期时间（秒），与JWT过期时间保持一致
        response.addCookie(cookie);
    }
    //4.从前端的请求里拿到Cookie里的token
    public static String getTokenFromCookie(HttpServletRequest request){
        Cookie[] cookies = request.getCookies();
        if (ObjUtil.isEmpty(cookies)){
            log.info("没有接收到Cookie或者Cookie为空");
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("token")){
                return cookie.getValue();
            }
        }
        log.info("从Cookie里没有找到token");
        return null;
    }

    //5.从redis拿到用户信息
//    public static LoginUserVO getLoginUserVOFromRedis(String token){
//        LoginUserVO bean = null;
//        try {
//            bean = JSONUtil.toBean(stringRedisTemplate.opsForValue().get(token), LoginUserVO.class);
//        }catch (Exception e){
//            log.info("从redis里没有找到用户信息");
//            ThrowUtil.throwBusinessException(CodeEnum.NOT_AUTH,"从redis里没有找到用户信息,token已过期或未登录");
//        }
//
//        return bean;
//    }


    public static UserVO getLoginUserVOFromCookie(HttpServletRequest request){
        StringRedisTemplate stringRedisTemplate = SpringContextHolder.getBean(StringRedisTemplate.class);
        //先从redis拿到登录用户的VO信息
        String tokenFromCookie = TokenUtil.getTokenFromCookie(request);
        String loginUserVOJson = stringRedisTemplate.opsForValue().get(tokenFromCookie);
        UserVO loginUserVO = JSONUtil.toBean(loginUserVOJson, UserVO.class);
        if (ObjUtil.isEmpty(loginUserVO)){
            log.info("没有找到用户信息");
            ThrowUtil.throwBusinessException(CodeEnum.NOT_AUTH,"没有找到用户信息");
        }
        return loginUserVO;
    }



}
