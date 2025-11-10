package com.zhao.zhaopicturebacked.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)// 指定注解只能用在方法上
@Retention(RetentionPolicy.RUNTIME) // 指定注解在运行时可用，这样AOP才能获取到
public @interface AuthType {

    int userType() default 0;// 用户权限 0-默认用户 1-管理员
}
