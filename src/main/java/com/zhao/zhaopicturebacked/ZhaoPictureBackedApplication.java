package com.zhao.zhaopicturebacked;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.zhao.zhaopicturebacked.mapper")
public class ZhaoPictureBackedApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZhaoPictureBackedApplication.class, args);
    }


}
