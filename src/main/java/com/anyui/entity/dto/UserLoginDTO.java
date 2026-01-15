package com.anyui.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户登录参数")
public class UserLoginDTO {

    @Schema(description = "用户名", example = "admin")
    private String username;

    @Schema(description = "密码", example = "123456")
    private String password;


}