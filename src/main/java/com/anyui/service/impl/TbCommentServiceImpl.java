package com.anyui.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.anyui.entity.SysUser;
import com.anyui.entity.TbComment;
import com.anyui.entity.TbPost;
import com.anyui.entity.dto.CommentAddDTO;
import com.anyui.entity.vo.CommentVO;
import com.anyui.mapper.TbCommentMapper;
import com.anyui.service.SysUserService;
import com.anyui.service.TbCommentService;
import com.anyui.service.TbPostService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TbCommentServiceImpl extends ServiceImpl<TbCommentMapper, TbComment> implements TbCommentService {

    @Autowired
    private SysUserService userService; // 查用户

    @Autowired
    private TbPostService postService; // 查帖子、更新评论数

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addComment(CommentAddDTO addDTO) {
        // 1. 获取当前登录用户
        long currentUserId = StpUtil.getLoginIdAsLong();

        // 2. 校验帖子是否存在
        TbPost post = postService.getById(addDTO.getPostId());
        if (post == null) {
            throw new RuntimeException("帖子不存在或已被删除");
        }

        // 3. 封装评论对象
        TbComment comment = new TbComment();
        // 这里会自动拷贝 content, replyUserId, postId, parentId (如果DTO里字段名一致)
        BeanUtils.copyProperties(addDTO, comment);

        comment.setUserId(currentUserId);
        comment.setCreateTime(LocalDateTime.now());

        // ---------------------------------------------------------
        // ✅ 新增逻辑：确保 parentId 有值
        // ---------------------------------------------------------
        if (addDTO.getParentId() != null) {
            comment.setParentId(addDTO.getParentId());
        } else {
            // 如果前端没传，或者传了 null，默认为 0 (表示这是顶层一级评论)
            comment.setParentId(0L);
        }

        // 4. 处理“回复”逻辑 (设置被回复人的昵称冗余字段)
        if (addDTO.getReplyUserId() != null && addDTO.getReplyUserId() > 0) {
            SysUser replyUser = userService.getById(addDTO.getReplyUserId());
            if (replyUser != null) {
                comment.setReplyUserName(replyUser.getNickname());
            } else {
                comment.setReplyUserName("未知用户");
            }
        }

        // 5. 保存评论
        this.save(comment);

        // 6. 更新帖子的评论数 (+1)
        post.setCommentCount(post.getCommentCount() + 1);
        postService.updateById(post);
    }

    @Override
    public List<CommentVO> getCommentList(Long postId) {
        // 1. 查询评论 (按时间正序，即楼层顺序)
        List<TbComment> commentList = this.lambdaQuery()
                .eq(TbComment::getPostId, postId)
                .orderByAsc(TbComment::getCreateTime)
                .list();

        if (commentList.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 收集所有评论人的 ID
        List<Long> userIds = commentList.stream()
                .map(TbComment::getUserId)
                .distinct()
                .collect(Collectors.toList());

        // 3. 批量查询用户信息
        Map<Long, SysUser> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));

        // 4. 转换 Entity -> VO
        List<CommentVO> voList = new ArrayList<>();
        for (TbComment comment : commentList) {
            CommentVO vo = new CommentVO();
            BeanUtils.copyProperties(comment, vo);

            // 填充评论人头像昵称
            SysUser user = userMap.get(comment.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar() == null ? "/static/default-avatar.png" : user.getAvatar());
            } else {
                vo.setNickname("未知用户");
                vo.setAvatar("/static/default-avatar.png");
            }

            // 被回复人昵称 replyUserName 已经在 save 时存入数据库了，
            // copyProperties 会自动拷贝，无需额外处理。

            voList.add(vo);
        }
        return voList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId) {
        // 1. 获取当前用户
        long currentUserId = StpUtil.getLoginIdAsLong();

        // 2. 查询评论
        TbComment comment = this.getById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }

        // 3. 权限校验
        if (!comment.getUserId().equals(currentUserId)) {
            throw new RuntimeException("无权删除他人的评论");
        }

        // 4. 删除评论
        this.removeById(commentId);

        // 5. 更新帖子评论数 (-1)
        // 既然我们要做完善，这里顺便把原来的遗憾补上
        TbPost post = postService.getById(comment.getPostId());
        if (post != null && post.getCommentCount() > 0) {
            post.setCommentCount(post.getCommentCount() - 1);
            postService.updateById(post);
        }
    }
}