package com.anyui.entity.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UserUpdateDTO {
    private String nickname;
    private String avatar;
    private Integer gender;
    // 密码修改建议单独做一个接口，为了安全
}