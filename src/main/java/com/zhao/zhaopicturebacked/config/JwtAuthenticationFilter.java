package com.zhao.zhaopicturebacked.config;

import cn.hutool.json.JSONUtil;
import com.zhao.zhaopicturebacked.enums.CodeEnum;
import com.zhao.zhaopicturebacked.model.LoginUserVO;
import com.zhao.zhaopicturebacked.utils.JWTUtil;
import com.zhao.zhaopicturebacked.utils.ThrowUtil;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        String relativePath = path.substring(contextPath.length());
        if (relativePath.contains("/doc.html")){
            return;
        }

        String bearerJwtToken = request.getHeader("Authorization");
        if (bearerJwtToken == null|| !bearerJwtToken.startsWith("Bearer ")){
            log.info("请求头不含正确格式的Authorization");
            ThrowUtil.throwBusinessException(CodeEnum.PARAMES_ERROR,"请求头不含正确格式的Authorization");
        }
        String jwtToken = bearerJwtToken.substring(7);
        String token = null;
        try {
            token = JWTUtil.getToken(jwtToken);
        } catch (Exception e) {
            log.error("JWT Token解析失败: {}", e.getMessage());
            ThrowUtil.throwBusinessException(CodeEnum.SYSTEM_ERROR,"解析token失败");
        }
        String loginUserVOJson = stringRedisTemplate.opsForValue().get(token);
        if (loginUserVOJson == null){
            log.info("token不存在或已过期");
            ThrowUtil.throwBusinessException(CodeEnum.NOT_AUTH,"token不存在或已过期");
        }
        LoginUserVO loginUserVO = JSONUtil.toBean(loginUserVOJson, LoginUserVO.class);
        if (loginUserVO != null && loginUserVO.getId() != null){
            filterChain.doFilter(request,response);
            return;
        }

        log.info("过滤器处理：用户未登录");
        ThrowUtil.throwBusinessException(CodeEnum.NOT_AUTH,"未登录");

    }
}