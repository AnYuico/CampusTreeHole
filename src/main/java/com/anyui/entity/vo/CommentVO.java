package com.anyui.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "评论列表VO")
public class CommentVO {

    @Schema(description = "评论ID")
    private Long id;

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

    @Schema(description = "被回复人ID")
    private Long replyUserId;

    @Schema(description = "评论时间")
    private LocalDateTime createTime;

    // ✅ 新增：审核状态 (前端可以用来给作者本人展示"审核中"标签)
    @Schema(description = "审核状态(0-待审核 1-通过 2-拒绝)")
    private Integer status;

    // ✅ 新增：拒绝原因 (前端可以用来给作者展示红色警告)
    @Schema(description = "拒绝原因")
    private String reason;

    // ✅ 新增：必须加这个，否则前端 goPostDetail(item.postId) 无法跳转
    @Schema(description = "所属帖子ID")
    private Long postId;

    // ✅ 新增：必须加这个，否则前端无法显示 "回复帖子：xxx"
    @Schema(description = "原帖内容摘要")
    private String postSummary;
}