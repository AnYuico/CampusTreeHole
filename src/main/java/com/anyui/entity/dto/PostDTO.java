package com.anyui.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class PostDTO {
    // ✅ 必须加上这个 ID 字段，修改时用到
    @Schema(description = "帖子ID (修改时必填)")
    private Long id;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "图片地址列表")
    private List<String> mediaUrls;

    @Schema(description = "是否匿名(0否 1是)")
    private Integer isAnonymous;
}
