package com.anyui.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "发布评论DTO")
public class CommentAddDTO {

    @Schema(description = "对应的帖子ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long postId;

    @Schema(description = "评论内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(description = "回复的目标用户ID (如果是直接评论帖子则为空)")
    private Long replyUserId;

    @Schema(description = "父评论ID (如果是回复某个评论，填该评论ID；如果是回复帖子，填0或不填)")
    private Long parentId;
}