package com.anyui.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "评论审核DTO")
public class CommentAuditDTO {

    @Schema(description = "评论ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long commentId;

    @Schema(description = "是否通过 (true-通过, false-拒绝)", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean pass;

    @Schema(description = "拒绝理由 (拒绝时必填)")
    private String reason;
}