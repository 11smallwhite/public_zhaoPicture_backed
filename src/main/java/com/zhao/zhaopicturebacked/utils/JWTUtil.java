package com.zhao.zhaopicturebacked.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

public class JWTUtil {
    private static final String YAN = "zhao";
    public static String getJWTToken(String token){
        String jwtToken = JWT.create().withClaim("jwtToken", token).sign(Algorithm.HMAC256(YAN));
        return jwtToken;
    }

    public static String getToken(String jwtToken){
        // 创建解析对象，使用的算法和secret要与创建token时保持一致
        JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256("zhao")).build();
        // 解析指定的token
        DecodedJWT decodedJWT = jwtVerifier.verify(jwtToken);
        // 获取解析后的token中的payload信息
        String token = decodedJWT.getClaim("jwtToken").asString();
        return token;
    }

}
