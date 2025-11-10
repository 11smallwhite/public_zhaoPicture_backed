package com.zhao.zhaopicturebacked.aop;


import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Component
@Aspect
@Slf4j
public class LoggingAspect {

    @Before("execution(* com.zhao.zhaopicturebacked.service.*.*(..))")
    public void logBeaforeMethod(JoinPoint joinPoint){
        //获取方法名
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        log.info("方法{}开始执行，参数为{}",methodName,args);
    }

    @AfterReturning(pointcut = "execution(* com.zhao.zhaopicturebacked.service.*.*(..))",returning = "result")
    public void logAfterMethod(JoinPoint joinPoint,Object result){
        //获取方法名
        String methodName = joinPoint.getSignature().getName();
        log.info("方法{}执行完毕，返回值为{}",methodName,result);
    }

    @AfterThrowing(pointcut = "execution(* com.zhao.zhaopicturebacked.service.*.*(..))",throwing = "e")
    public void logAfterThrowingMethod(JoinPoint joinPoint,Exception e){
        //获取方法名
        String methodName = joinPoint.getSignature().getName();
        log.info("方法{}执行异常，异常信息为{}",methodName,e);
    }
}
