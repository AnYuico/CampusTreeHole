package com.anyui.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List; // ✅ 导包

@Data
@Schema(description = "发布树洞DTO")
public class PostAddDTO {

    @Schema(description = "帖子内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(description = "分类 (日常, 吐槽, 表白...)", example = "日常")
    private String category;

    @Schema(description = "是否匿名 (0:否 1:是)", defaultValue = "0")
    private Integer isAnonymous;

    // ✅ 修改：改为 List<String>
    // 能够直接接收前端传来的 ["http://xxx/1.jpg", "http://xxx/2.jpg"]
    @Schema(description = "配图地址列表")
    private List<String> mediaUrls;
}