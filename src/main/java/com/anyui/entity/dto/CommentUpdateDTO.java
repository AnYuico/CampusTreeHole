package com.anyui.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import jakarta.validation.constraints.NotBlank; // 如果是Spring Boot 2.x用 javax.validation
import jakarta.validation.constraints.NotNull;

@Data
@Schema(description = "评论修改DTO")
public class CommentUpdateDTO {

    @Schema(description = "评论ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "评论ID不能为空")
    private Long id;

    @Schema(description = "修改后的内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "评论内容不能为空")
    private String content;
}