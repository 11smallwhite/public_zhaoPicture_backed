package com.zhao.zhaopicturebacked.task;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 随时取spring bean的工具类
 */
@Slf4j
@Component
public class SpringContextHolder implements ApplicationContextAware {
    private static ApplicationContext applicationContext;

    /**
     * Sping容器初始化积木了ApplicationContextAware接口的类时，会自动调用setApplicationContext方法
     * @param springApplicationContext
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext springApplicationContext) throws BeansException {
        applicationContext = springApplicationContext;
    }


    /**
     * 校验applicationContext是否为空
     * 获取applicationContext
     * @return
     */
    public static void checkApplicationContext() {
        if (applicationContext == null) {
            throw new IllegalStateException("applicaitonContext未注入,请在applicationContext.xml中定义SpringContextHolder");
        }
    }

    /**
     * 根据beanName获取bean
     * @param name
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T getBean(String name){
        checkApplicationContext();
        Object bean = applicationContext.getBean(name);
        return (T) bean;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getBean(Class<T> clazz){
        Map<String, T> beanMap = applicationContext.getBeansOfType(clazz);
        if(CollUtil.isNotEmpty(beanMap)){
            return beanMap.values().iterator().next();
        }else{
            log.info("未找到该bean");
            return null;
        }

    }


}
