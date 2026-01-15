package com.anyui.controller;

import com.anyui.common.Result;
import com.anyui.entity.dto.PostLikeDTO;
import com.anyui.service.TbPostLikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "点赞模块")
@RestController
@RequestMapping("/like")
public class TbPostLikeController {

    @Autowired
    private TbPostLikeService postLikeService;

    @Operation(summary = "点赞 或 取消点赞")
    @PostMapping("/toggle")
    public Result<String> toggleLike(@RequestBody PostLikeDTO likeDTO) {
        // 判空 (虽然 DTO 里应该处理，但这里为了稳健可以加一层)
        if (likeDTO.getPostId() == null) {
            return Result.error("帖子ID不能为空");
        }

        // 业务逻辑全部移交 Service
        postLikeService.toggleLike(likeDTO);

        return Result.success("操作成功");
    }
}