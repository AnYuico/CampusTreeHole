package com.anyui.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户注册参数")
public class UserRegisterDTO {

    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "确认密码")
    private String checkPassword;

    @Schema(description = "昵称", example = "快乐小狗")
    private String nickname;

    @Schema(description = "性别 0:保密 1:男 2:女", example = "1")
    private Integer gender;
}