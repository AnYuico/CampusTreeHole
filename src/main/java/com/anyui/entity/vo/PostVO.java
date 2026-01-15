package com.anyui.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List; // ✅ 导包

@Data
@Schema(description = "树洞列表展示对象")
public class PostVO {

    private Long id;
    private Long userId;
    private String category;
    private String content;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer isAnonymous;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "发布人昵称")
    private String nickname;

    @Schema(description = "发布人头像")
    private String avatar;

    @Schema(description = "当前用户是否已点赞 (0:否 1:是)")
    private Boolean isLiked = false;

    // ✅ 修改：改为 List<String>
    // 这样前端收到的是: "mediaUrls": ["http://x.jpg", "http://y.jpg"]
    @Schema(description = "图片/视频地址列表")
    private List<String> mediaUrls;
}