package com.anyui.common;

import cn.dev33.satoken.exception.NotLoginException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. 拦截 Sa-Token 未登录异常 (保持你原有的精细化处理)
    @ExceptionHandler(NotLoginException.class)
    public Result<String> handlerNotLoginException(NotLoginException nle) {
        String message = "";
        if(nle.getType().equals(NotLoginException.NOT_TOKEN)) {
            message = "未提供Token，请登录";
        } else if(nle.getType().equals(NotLoginException.INVALID_TOKEN)) {
            message = "Token无效，请重新登录";
        } else if(nle.getType().equals(NotLoginException.TOKEN_TIMEOUT)) {
            message = "Token已过期，请重新登录";
        } else if(nle.getType().equals(NotLoginException.BE_REPLACED)) {
            message = "您的账号已在别处登录，您被迫下线";
        } else if(nle.getType().equals(NotLoginException.KICK_OUT)) {
            message = "您已被强制下线";
        } else {
            message = "当前会话未登录";
        }
        return Result.error(401, message); // 401 状态码非常规范
    }

    // 2. 拦截所有运行时异常 (处理你 deletePost 抛出的错误)
    @ExceptionHandler(RuntimeException.class)
    public Result<String> handleRuntimeException(RuntimeException e) {
        e.printStackTrace();
        // 如果 message 为空，返回默认提示，否则返回具体的业务错误信息
        String msg = (e.getMessage() == null || e.getMessage().isEmpty()) ? "系统运行异常" : e.getMessage();
        return Result.error(500, msg);
    }

    // 3. 拦截其他未知异常
    @ExceptionHandler(Exception.class)
    public Result<String> handlerException(Exception e) {
        e.printStackTrace();
        return Result.error(500, "服务器开小差了，请稍后再试");
    }
}