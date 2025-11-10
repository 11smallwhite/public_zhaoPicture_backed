package com.zhao.zhaopicturebacked;

import cn.hutool.core.io.FileUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import com.zhao.zhaopicturebacked.config.CosClientConfig;
import com.zhao.zhaopicturebacked.cos.CosService;
import com.zhao.zhaopicturebacked.service.PictureService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


import javax.annotation.Resource;
import java.io.File;
import java.util.Calendar;
import java.util.HashMap;

@SpringBootTest
class ZhaoPictureBackedApplicationTests {

    @Resource
    private CosClientConfig cosclientConfig;


    @Resource
    private CosService cosService;



    @Test
    public void downloadPicture(){
        File tempFile = FileUtil.createTempFile();
        String key =  String.format("/public/1986704662206394370.logo.avif");
        ObjectMetadata picturetoFile = cosService.getPicturetoFile(key, tempFile);
    }


    @Test
    public void delete(){
        COSClient client = cosclientConfig.getClient();
        client.deleteObject(cosclientConfig.getBucket(), "/public/1983806652908695554.dddd.jpg");
    }


    @Test
    public void testGenerateToken(){
        // 指定token过期时间为10秒
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, 10);

        String token = JWT.create()
                .withHeader(new HashMap<>())  // Header
                .withClaim("userId", 21)  // Payload
                .withClaim("userName", "baobao")
                .withExpiresAt(calendar.getTime())  // 过期时间
                .sign(Algorithm.HMAC256("zhao"));  // 签名用的secret
        System.out.println(token);
        // 创建解析对象，使用的算法和secret要与创建token时保持一致
        JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256("zhao")).build();
        // 解析指定的token
        DecodedJWT decodedJWT = jwtVerifier.verify(token);
        // 获取解析后的token中的payload信息
        Claim userId = decodedJWT.getClaim("userId");
        Claim userName = decodedJWT.getClaim("userName");

        System.out.println(userId.asInt());
        System.out.println(userName.asString());
// 输出超时时间
        System.out.println(decodedJWT.getExpiresAt());
    }



}
