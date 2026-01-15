package com.anyui.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "统一返回结果")
public class Result<T> {

    @Schema(description = "状态码: 200成功, 其他失败")
    private Integer code;

    @Schema(description = "错误信息")
    private String msg;

    @Schema(description = "返回数据")
    private T data;

    // 成功方法
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.code = 200;
        result.msg = "操作成功";
        result.data = data;
        return result;
    }

    // 默认错误方法 (500)
    public static <T> Result<T> error(String msg) {
        return error(500, msg);
    }

    // 支持自定义状态码的错误方法 (为了配合 ExceptionHandler)
    public static <T> Result<T> error(Integer code, String msg) {
        Result<T> result = new Result<>();
        result.code = code;
        result.msg = msg;
        return result;
    }
}