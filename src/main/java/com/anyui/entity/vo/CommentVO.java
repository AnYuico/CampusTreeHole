package com.anyui.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "评论列表VO")
public class CommentVO {

    @Schema(description = "评论ID")
    private Long id;

    // ✅ 必须加上这个！前端靠它来组装楼中楼
    @Schema(description = "父评论ID")
    private Long parentId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "用户头像")
    private String avatar;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "被回复人昵称")
    private String replyUserName;

    // 如果有 replyUserId 也可以加上，双重保险
    @Schema(description = "被回复人ID")
    private Long replyUserId;

    @Schema(description = "评论时间")
    private LocalDateTime createTime;
}