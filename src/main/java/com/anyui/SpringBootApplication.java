package com.anyui;

import org.mybatis.spring.annotation.MapperScan; // 1. 引入这个包
import org.springframework.boot.SpringApplication;

@org.springframework.boot.autoconfigure.SpringBootApplication
@MapperScan("com.anyui.mapper")
public class SpringBootApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringBootApplication.class);
    }
}