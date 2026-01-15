package com.anyui.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 评论表
 * </p>
 *
 * @author AnyUI
 * @since 2025-12-25
 */
@Getter
@Setter
@TableName("tb_comment")
@Schema(name = "TbComment", description = "评论表")
public class TbComment implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "所属帖子ID")
    @TableField("post_id")
    private Long postId;

    @Schema(description = "评论人ID")
    @TableField("user_id")
    private Long userId;

    @Schema(description = "评论内容")
    @TableField("content")
    private String content;

    @Schema(description = "被回复人ID (若为0或NULL则表示直接回复帖子)")
    @TableField("reply_user_id")
    private Long replyUserId;

    @Schema(description = "被回复人昵称 (冗余字段，避免多表联查)")
    @TableField("reply_user_name")
    private String replyUserName;

    @Schema(description = "评论时间")
    @TableField("create_time")
    private LocalDateTime createTime;

    @Schema(description = "父评论id")
    @TableField("parent_id")
    private Long parentId;
}
