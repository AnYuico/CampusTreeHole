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
 * 点赞记录表
 * </p>
 *
 * @author AnyUI
 * @since 2025-12-25
 */
@Getter
@Setter
@TableName("tb_post_like")
@Schema(name = "TbPostLike", description = "点赞记录表")
public class TbPostLike implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "帖子ID")
    @TableField("post_id")
    private Long postId;

    @Schema(description = "点赞人ID")
    @TableField("user_id")
    private Long userId;

    @TableField("create_time")
    private LocalDateTime createTime;
}
