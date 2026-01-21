package com.anyui.controller;

import com.anyui.common.Result;
import com.anyui.entity.dto.CommentAddDTO;
import com.anyui.entity.dto.CommentUpdateDTO;
import com.anyui.entity.vo.CommentVO;
import com.anyui.service.TbCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "用户评论模块")
@RestController
@RequestMapping("/comment")
public class TbCommentController {

    @Autowired
    private TbCommentService commentService;

    @Operation(summary = "发表评论")
    @PostMapping("/add")
    public Result<String> add(@RequestBody CommentAddDTO addDTO) {
        if (addDTO.getPostId() == null || addDTO.getContent() == null) {
            return Result.error("参数不完整");
        }

        // 业务逻辑全部移交 Service
        commentService.addComment(addDTO);

        return Result.success("评论成功");
    }

    @Operation(summary = "获取某帖子的评论列表")
    @GetMapping("/list")
    public Result<List<CommentVO>> list(@RequestParam Long postId) {
        // 业务逻辑全部移交 Service
        List<CommentVO> list = commentService.getCommentList(postId);

        return Result.success(list);
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam Long commentId) {
        // 业务逻辑全部移交 Service (包含权限校验)
        commentService.deleteComment(commentId);

        return Result.success("删除成功");
    }

    @Operation(summary = "修改评论(并重新触发审核)")
    @PostMapping("/update")
    public Result<String> updateComment(@RequestBody CommentUpdateDTO updateDTO) {
        commentService.updateComment(updateDTO);
        return Result.success("修改成功，请等待审核");
    }

    @Operation(summary = "获取我的评论列表")
    @GetMapping("/listMy")
    public Result<List<CommentVO>> listMyComments() {
        List<CommentVO> list = commentService.listMyComments();
        return Result.success(list);
    }
}