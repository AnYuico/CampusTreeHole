package com.anyui.entity.dto;

import lombok.Data;

@Data
public class PostAuditDTO {
    /**
     * 帖子ID
     */
    private Long postId;

    /**
     * 是否通过 (true:通过, false:拒绝)
     */
    private Boolean pass;

    /**
     * 拒绝原因 (仅拒绝时需要)
     */
    private String reason;
}