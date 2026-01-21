package com.anyui.service;

import com.anyui.entity.TbComment;
import com.anyui.entity.dto.CommentAddDTO;
import com.anyui.entity.dto.CommentUpdateDTO;
import com.anyui.entity.vo.CommentVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface TbCommentService extends IService<TbComment> {

    /**
     * 发表评论
     * @param addDTO 评论信息
     */
    void addComment(CommentAddDTO addDTO);

    /**
     * 获取某帖子的评论列表
     * @param postId 帖子ID
     * @return 评论VO列表
     */
    List<CommentVO> getCommentList(Long postId);

    /**
     * 删除评论
     * @param commentId 评论ID
     */
    void deleteComment(Long commentId);

    /**
     * 修改评论
     * @param updateDTO
     */
    void updateComment(CommentUpdateDTO updateDTO);

    List<CommentVO> listMyComments();


    /**
     * 获取待审核评论列表 (带原帖摘要)
     */
    List<CommentVO> getPendingComments();

    /**
     * 执行评论审核
     * @param commentId 评论ID
     * @param pass 是否通过
     * @param reason 拒绝理由
     */
    void auditComment(Long commentId, Boolean pass, String reason);
}