package com.anyui.entity.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PostLikeDTO {
    @Schema(description = "帖子ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long postId;
}