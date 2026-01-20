package com.anyui.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.anyui.entity.TbPost;
import com.anyui.entity.TbPostLike;
import com.anyui.entity.dto.PostLikeDTO;
import com.anyui.mapper.TbPostLikeMapper;
import com.anyui.service.AiAuditService;
import com.anyui.service.TbPostLikeService;
import com.anyui.service.TbPostService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TbPostLikeServiceImpl extends ServiceImpl<TbPostLikeMapper, TbPostLike> implements TbPostLikeService {

    @Autowired
    private TbPostService postService; // 需要操作帖子表




    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleLike(PostLikeDTO likeDTO) {
        // 1. 获取当前登录用户 (从 Token 获取)
        long currentUserId = StpUtil.getLoginIdAsLong();
        Long postId = likeDTO.getPostId();

        // 2. 检查帖子是否存在
        TbPost post = postService.getById(postId);
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }

        // 3. 查询是否已经点过赞
        LambdaQueryWrapper<TbPostLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TbPostLike::getUserId, currentUserId);
        wrapper.eq(TbPostLike::getPostId, postId);
        TbPostLike existLike = this.getOne(wrapper);

        // 4. 执行逻辑
        if (existLike != null) {
            // --- 情况A：已经点过赞 -> 取消点赞 ---
            this.removeById(existLike.getId());

            // 更新帖子点赞数 -1
            if (post.getLikeCount() > 0) {
                post.setLikeCount(post.getLikeCount() - 1);
            }
        } else {
            // --- 情况B：没点过赞 -> 新增点赞 ---
            TbPostLike newLike = new TbPostLike();
            newLike.setPostId(postId);
            newLike.setUserId(currentUserId);
            newLike.setCreateTime(LocalDateTime.now());
            this.save(newLike);

            // 更新帖子点赞数 +1
            post.setLikeCount(post.getLikeCount() + 1);
        }

        // 5. 同步更新帖子表
        postService.updateById(post);
    }
}