package com.zhao.zhaopicturebacked.aop;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.zhao.zhaopicturebacked.annotation.AuthType;
import com.zhao.zhaopicturebacked.common.UserConstant;
import com.zhao.zhaopicturebacked.enums.CodeEnum;
import com.zhao.zhaopicturebacked.model.LoginUserVO;
import com.zhao.zhaopicturebacked.utils.ThrowUtil;
import com.zhao.zhaopicturebacked.utils.TokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

@Aspect//声明这是一个切面类
@Component//将该类交给spring管理
@Slf4j
public class AuthCheckAop {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private HttpServletResponse response;

    @Around("@annotation(ddddddd)")//@Around：环绕通知 表示在方法执行之前进行拦截,
    // @annotation表示只拦截方法，并且它的参数ddddddd与方法里的参数进行匹配，决定了要拦截的注解是什么，permissionCheck方法里的参数ddddddd的类型是AuthType，
    // 这告诉了@annotation要拦截的是拥有AuthType注解的方法
    public Object permissionCheck(ProceedingJoinPoint joinPoint, AuthType ddddddd) throws Throwable {
        //1.拿到HttpServeletRequest，然后通过它拿到用户Type信息
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();// 通过Spring的RequestContextHolder获取当前线程的HTTP请求
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        //2.从request里拿到Cookie里的token
        String token = TokenUtil.getTokenFromCookie(request);
        if (ObjUtil.isEmpty(token)){
            log.info("token为空，为登录");
            ThrowUtil.throwBusinessException(CodeEnum.NOT_AUTH,"未登录");
        }
        //3.从redis里拿到用户信息
        String loginUserVOJson = stringRedisTemplate.opsForValue().get(token);
        //3.1续期redis和token
        stringRedisTemplate.expire(token,60*60, TimeUnit.SECONDS);
        //续期cookie
        TokenUtil.setTokenToCookie(token, response);
        if (ObjUtil.isEmpty(loginUserVOJson)){
            log.info("注解AOP验证未通过，从redis里没有拿到用户信息");
            ThrowUtil.throwBusinessException(CodeEnum.NOT_AUTH,"未登录");
        }
        LoginUserVO loginUserVO = JSONUtil.toBean(loginUserVOJson, LoginUserVO.class);
        //2.拿到注解上的userType,低于userType的权限不能通过
        int userType = ddddddd.userType();

        if (loginUserVO.getUserType()<userType){
            log.info("注解+AOP权限校验未通过，用户权限不足");
            ThrowUtil.throwBusinessException(CodeEnum.NOT_AUTH,"权限不足");
        }

        return joinPoint.proceed();
    }
}
