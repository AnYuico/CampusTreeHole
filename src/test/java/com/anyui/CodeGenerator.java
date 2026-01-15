package com.anyui;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.Scanner;

public class CodeGenerator {

    // 数据库配置
    private static final String URL = "jdbc:mysql://121.40.160.208:3305/campus_tree_hole?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "tzp1911";

    public static void main(String[] args) {
        // 获取项目根目录
        String projectPath = System.getProperty("user.dir");

        // 交互式输入表名
        System.out.println("请输入要生成的表名（多个表用英文逗号隔开，输入 all 生成所有表）：");
        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine();

        // 开始生成
        FastAutoGenerator.create(URL, USERNAME, PASSWORD)

                // 1. 全局配置
                .globalConfig(builder -> {
                    builder.author("AnyUI") // 设置作者
                            .enableSpringdoc()

                            .disableOpenDir() // 禁止打开输出目录
                            .dateType(DateType.TIME_PACK) // 使用 Java 8 时间类型
                            .outputDir(Paths.get(projectPath, "src/main/java").toString()); // 指定输出目录
                })

                // 2. 包配置
                .packageConfig(builder -> {
                    builder.parent("com.anyui") // 设置父包名
                            // .moduleName("system")
                            .entity("entity")
                            .service("service")
                            .serviceImpl("service.impl")
                            .mapper("mapper")
                            .xml("mapper.xml")
                            .controller("controller")
                            .pathInfo(Collections.singletonMap(OutputFile.xml, Paths.get(projectPath, "src/main/resources/mapper").toString()));
                })

                // 3. 策略配置
                .strategyConfig(builder -> {
                    // 处理表名输入
                    if (!"all".equalsIgnoreCase(input)) {
                        builder.addInclude(input.split(","));
                    }

                    // Entity 策略
                    builder.entityBuilder()
                            .enableLombok()
                            .enableTableFieldAnnotation()
                            .logicDeleteColumnName(null)
                            .enableFileOverride(); // 允许覆盖已生成的文件

                    // Controller 策略
                    builder.controllerBuilder()
                            .enableRestStyle()
                            .enableHyphenStyle();

                    // Service 策略
                    builder.serviceBuilder()
                            .formatServiceFileName("%sService");
                })
                .templateEngine(new VelocityTemplateEngine()) // 使用 Velocity 引擎
                .execute();

        System.out.println("代码生成完毕！请刷新项目目录。");
    }
}