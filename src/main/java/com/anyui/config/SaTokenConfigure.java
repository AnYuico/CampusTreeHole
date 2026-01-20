package com.anyui.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {
    /**
     * 注册 Sa-Token 的拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，打开注解式鉴权功能
        registry.addInterceptor(new SaInterceptor(handle -> {
                    // 这里可以写全局的校验逻辑，比如强制所有接口登录：
                    // StpUtil.checkLogin();
                    // 但我们目前主要依靠注解，所以这里留空即可
                })
                        .isAnnotation(true)) // ✅ 关键修改：开启注解鉴权 (支持 @SaCheckRole 等)
                .addPathPatterns("/**");
    }
}