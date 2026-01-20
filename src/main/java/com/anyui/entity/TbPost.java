package com.anyui.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler; // ✅ 记得导包
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List; // ✅ 记得导包

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 关键点1：必须开启 autoResultMap，否则查询时 JSON 转 List 会失败
@TableName(value = "tb_post", autoResultMap = true)
@Schema(name = "TbPost", description = "帖子表")
public class TbPost implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "发帖人ID")
    @TableField("user_id")
    private Long userId;

    @Schema(description = "分类(如: 表白墙, 吐槽, 闲置)")
    @TableField("category")
    private String category;

    @Schema(description = "帖子内容")
    @TableField("content")
    private String content;

    // 关键点2：类型改为 List<String>
    // 关键点3：指定 TypeHandler
    @Schema(description = "图片/视频链接，JSON数组格式存储")
    @TableField(value = "media_urls", typeHandler = JacksonTypeHandler.class)
    private List<String> mediaUrls;

    @Schema(description = "浏览量")
    @TableField("view_count")
    private Integer viewCount;

    @Schema(description = "点赞数")
    @TableField("like_count")
    private Integer likeCount;

    @Schema(description = "评论数")
    @TableField("comment_count")
    private Integer commentCount;

    @Schema(description = "是否匿名 0:否 1:是")
    @TableField("is_anonymous")
    private Integer isAnonymous;

    @Schema(description = "发布时间")
    @TableField("create_time")
    private LocalDateTime createTime;

    @Schema(description = "状态:0待审核,1通过,2拒绝")
    @TableField("status")
    private Integer status;

    @Schema(description = "审核拒绝原因")
    @TableField("reason")
    private String reason;
}