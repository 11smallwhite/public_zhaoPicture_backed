package com.zhao.zhaopicturebacked.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 覆盖所有请求
        registry
                .addMapping("/**")//指定后端哪些接口路径允许被跨域访问。
                // 允许发送 Cookie
                .allowCredentials(true)//允许前端发送凭据（Cookies、HTTP认证等）
                .allowedOriginPatterns("*")// 指定哪些前端的域名被允许访问这些接口。（必须用 patterns，否则 * 会和 allowCredentials 冲突）
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")//允许的 HTTP 方法
                .allowedHeaders("*")//允许前端发送的请求头
                .exposedHeaders("*");//允许前端访问的响应头
    }

}
