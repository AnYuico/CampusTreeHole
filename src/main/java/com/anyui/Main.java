package com.anyui;

import org.mybatis.spring.annotation.MapperScan; // 1. 引入这个包
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.anyui.mapper")
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class);
    }
}