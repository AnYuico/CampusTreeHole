package com.anyui.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.anyui.entity.SysUser;
import com.anyui.entity.TbComment;
import com.anyui.entity.TbPost;
import com.anyui.entity.dto.AuditResult;
import com.anyui.entity.dto.CommentAddDTO;
import com.anyui.entity.dto.CommentUpdateDTO;
import com.anyui.entity.vo.CommentVO;
import com.anyui.mapper.TbCommentMapper;
import com.anyui.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TbCommentServiceImpl extends ServiceImpl<TbCommentMapper, TbComment> implements TbCommentService {

    @Autowired
    private SysUserService userService; // 查用户

    @Autowired
    private TbPostService postService; // 查帖子、更新评论数

    // ✅ 1. 注入 AI 审核服务
    @Autowired
    private AiAuditService aiAuditService;

    @Autowired
    private SysConfigService sysConfigService;

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
        BeanUtils.copyProperties(addDTO, comment);
        comment.setUserId(currentUserId);
        comment.setCreateTime(LocalDateTime.now());

        // 处理 parentId
        if (addDTO.getParentId() != null) {
            comment.setParentId(addDTO.getParentId());
        } else {
            comment.setParentId(0L);
        }

        // 处理“回复”逻辑
        if (addDTO.getReplyUserId() != null && addDTO.getReplyUserId() > 0) {
            SysUser replyUser = userService.getById(addDTO.getReplyUserId());
            if (replyUser != null) {
                comment.setReplyUserName(replyUser.getNickname());
            } else {
                comment.setReplyUserName("未知用户");
            }
        }

        // 调用下面的私有方法，重置状态并进行 AI 检查
        this.processAiAudit(comment);

        // 4. 保存评论
        this.save(comment);

        // 5. 更新帖子的评论数 (+1)
        // 只有审核通过(status=1)或者是待审核(status=0)的，才算入评论数？
        // 这里的逻辑看你需求。通常为了防止被拒的评论刷数据，建议只有 status=1 才+1
        // 但为了简单，暂时只要发了就+1，或者你可以加个 if(comment.getStatus() == 1) 判断
        post.setCommentCount(post.getCommentCount() + 1);
        postService.updateById(post);
    }

    @Override
    public List<CommentVO> getCommentList(Long postId) {
        // ---------------------------------------------------------
        // ✅ 3. 修改查询逻辑：权限过滤
        // 规则：如果是审核通过(status=1) -> 所有人可见
        //       如果是未通过/待审核 -> 只有作者自己可见
        // ---------------------------------------------------------

        // 获取当前查看者的ID (如果未登录则为 null)
        Long viewUserId = null;
        if (StpUtil.isLogin()) {
            viewUserId = StpUtil.getLoginIdAsLong();
        }

        LambdaQueryWrapper<TbComment> query = new LambdaQueryWrapper<>();
        query.eq(TbComment::getPostId, postId); // 基础条件：属于该帖子

        // 核心过滤条件：(status = 1) OR (user_id = 当前用户ID)
        // 必须用 and(...) 包裹，否则会和前面的 eq 变成并列关系，导致逻辑错误
        Long finalViewUserId = viewUserId;
        query.and(wrapper -> {
            wrapper.eq(TbComment::getStatus, 1); // 条件A: 审核通过
            if (finalViewUserId != null) {
                wrapper.or().eq(TbComment::getUserId, finalViewUserId); // 条件B: 是我看自己的
            }
        });

        query.orderByAsc(TbComment::getCreateTime); // 按时间楼层排序

        List<TbComment> commentList = this.list(query);

        if (commentList.isEmpty()) {
            return new ArrayList<>();
        }

        // 下面这部分 Entity 转 VO 的逻辑保持不变
        List<Long> userIds = commentList.stream()
                .map(TbComment::getUserId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, SysUser> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));

        List<CommentVO> voList = new ArrayList<>();
        for (TbComment comment : commentList) {
            CommentVO vo = new CommentVO();
            // 这里会自动把 Entity 里的 status 和 reason 拷贝到 VO 里
            BeanUtils.copyProperties(comment, vo);

            SysUser user = userMap.get(comment.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar() == null ? "/static/default-avatar.png" : user.getAvatar());
            } else {
                vo.setNickname("未知用户");
                vo.setAvatar("/static/default-avatar.png");
            }
            voList.add(vo);
        }
        return voList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId) {
        // 此方法逻辑无需修改，保持原样即可
        long currentUserId = StpUtil.getLoginIdAsLong();
        TbComment comment = this.getById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }
        if (!comment.getUserId().equals(currentUserId)) {
            throw new RuntimeException("无权删除他人的评论");
        }
        this.removeById(commentId);

        TbPost post = postService.getById(comment.getPostId());
        if (post != null && post.getCommentCount() > 0) {
            post.setCommentCount(post.getCommentCount() - 1);
            postService.updateById(post);
        }
    }

    /**
     * 修改评论
     * @param updateDTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateComment(CommentUpdateDTO updateDTO) {
        // 1. 查询原评论
        TbComment comment = this.getById(updateDTO.getId());
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }

        // 2. 🔒 权限校验：只能修改自己的评论
        long currentUserId = StpUtil.getLoginIdAsLong();
        if (!comment.getUserId().equals(currentUserId)) {
            throw new RuntimeException("无权修改他人评论");
        }

        // 3. 更新内容
        // 注意：postId, parentId, replyUserId 等关系字段严禁修改
        comment.setContent(updateDTO.getContent());

        // 4. 核心逻辑：修改后必须重新审核！
        // 调用下面的私有方法，重置状态并进行 AI 检查
        this.processAiAudit(comment);

        // 5. 执行更新
        this.updateById(comment);
    }

    @Override
    public List<CommentVO> listMyComments() {
        // 1. 获取当前登录用户ID
        long currentUserId = StpUtil.getLoginIdAsLong();

        // 2. 查询该用户的所有评论 (按时间倒序)
        List<TbComment> commentList = this.lambdaQuery()
                .eq(TbComment::getUserId, currentUserId)
                .orderByDesc(TbComment::getCreateTime)
                .list();

        if (commentList.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. 收集所有关联的 PostID (用于批量查帖子)
        Set<Long> postIds = commentList.stream()
                .map(TbComment::getPostId)
                .collect(Collectors.toSet());

        // 4. 批量查询帖子信息
        Map<Long, TbPost> postMap = new HashMap<>();
        if (!postIds.isEmpty()) {
            List<TbPost> posts = postService.listByIds(postIds);
            // 转成 Map<PostId, TbPost> 方便后续查找
            postMap = posts.stream().collect(Collectors.toMap(TbPost::getId, p -> p));
        }

        // 5. 准备用户信息 (虽然是查自己的，但为了 VO 格式统一，还是填一下)
        // 也可以直接查一次 userService.getById(currentUserId)
        SysUser currentUser = userService.getById(currentUserId);
        String nickname = (currentUser != null) ? currentUser.getNickname() : "我";
        String avatar = (currentUser != null) ? currentUser.getAvatar() : "";

        // 6. 组装 VO 列表
        List<CommentVO> voList = new ArrayList<>();
        for (TbComment comment : commentList) {
            CommentVO vo = new CommentVO();
            // 复制基础属性 (id, content, createTime, status, reason, postId 等)
            BeanUtils.copyProperties(comment, vo);

            // 填充用户信息
            vo.setNickname(nickname);
            vo.setAvatar(avatar);

            // 填充原帖摘要
            TbPost post = postMap.get(comment.getPostId());
            if (post != null) {
                // 截取前 20 个字
                String postContent = post.getContent();
                if (postContent != null && !postContent.isEmpty()) {
                    vo.setPostSummary(postContent.length() > 20 ? postContent.substring(0, 20) + "..." : postContent);
                } else {
                    // 如果帖子没有文字只有图片
                    vo.setPostSummary("[图片/视频分享]");
                }
            } else {
                vo.setPostSummary("该帖子已被删除");
            }

            voList.add(vo);
        }

        return voList;
    }

    @Override
    public List<CommentVO> getPendingComments() {
        // 1. 查询所有 status = 0 (待审核) 的评论，按时间正序（先发的先审）
        List<TbComment> list = this.lambdaQuery()
                .eq(TbComment::getStatus, 0)
                .orderByAsc(TbComment::getCreateTime)
                .list();

        if (list.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 批量查原帖 (关键步骤：管理员需要知道他在评论什么)
        Set<Long> postIds = list.stream().map(TbComment::getPostId).collect(Collectors.toSet());
        Map<Long, TbPost> postMap = new HashMap<>();
        if (!postIds.isEmpty()) {
            // listByIds 是 MyBatis-Plus 自带的批量查询
            postMap = postService.listByIds(postIds).stream()
                    .collect(Collectors.toMap(TbPost::getId, p -> p));
        }

        // 3. 批量查用户 (显示是谁发的)
        Set<Long> userIds = list.stream().map(TbComment::getUserId).collect(Collectors.toSet());
        Map<Long, SysUser> userMap = new HashMap<>();
        if(!userIds.isEmpty()){
            userMap = userService.listByIds(userIds).stream()
                    .collect(Collectors.toMap(SysUser::getId, u -> u));
        }

        // 4. 组装 VO
        List<CommentVO> voList = new ArrayList<>();
        for (TbComment comment : list) {
            CommentVO vo = new CommentVO();
            BeanUtils.copyProperties(comment, vo);

            // 填充发布人信息
            SysUser user = userMap.get(comment.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar());
            } else {
                vo.setNickname("未知用户");
            }

            // ✅ 填充原帖摘要 (这是审核的关键上下文)
            TbPost post = postMap.get(comment.getPostId());
            if (post != null) {
                String content = post.getContent();
                // 截取前20个字作为摘要，太长了界面不好看
                vo.setPostSummary(content != null && content.length() > 20
                        ? content.substring(0, 20) + "..."
                        : content);
            } else {
                vo.setPostSummary("【原帖已删除】");
            }

            voList.add(vo);
        }
        return voList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditComment(Long commentId, Boolean pass, String reason) {
        // 1. 查是否存在
        TbComment comment = this.getById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在或已被删除");
        }

        // 2. 更新状态
        if (pass) {
            comment.setStatus(1); // 通过
            comment.setReason("");
        } else {
            comment.setStatus(2); // 拒绝
            comment.setReason(reason);
        }

        // 3. 执行更新
        this.updateById(comment);
    }

    // ==================== ⬇️ 抽取出来的通用审核逻辑 ⬇️ ====================

    /**
     * 执行AI审核并设置评论状态
     * 修复点：AI关闭时，必须降级为人工审核(0)，绝不能直接通过(1)！
     */
    private void processAiAudit(TbComment comment) {
        // 1. 检查全局 AI 开关状态
        boolean isAiOpen = sysConfigService.isAiAuditEnabled();

        if (!isAiOpen) {
            // ✅ 修正逻辑：AI 关闭 -> 转入人工审核 (Status = 0)
            comment.setStatus(0);
            comment.setReason("AI审核功能已关闭，转入人工审核");
            return;
        }

        // 2. AI 开启，正常调用
        try {
            AuditResult result = aiAuditService.auditText(comment.getContent());

            if (result.isPass()) {
                comment.setStatus(1); // 通过
                comment.setReason("");
            } else {
                comment.setStatus(2); // 拒绝
                comment.setReason(result.getReason());
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 🛑 服务降级：AI 服务异常 -> 转入人工审核
            comment.setStatus(0);
            comment.setReason("AI服务连接超时，转入人工审核");
        }
    }
}