package com.anyui.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.anyui.common.PostCategoryConstants;
import com.anyui.entity.SysUser;
import com.anyui.entity.TbPost;
import com.anyui.entity.TbPostLike;
import com.anyui.entity.dto.PostAddDTO;
import com.anyui.entity.vo.PostVO;
import com.anyui.mapper.TbPostLikeMapper;
import com.anyui.mapper.TbPostMapper;
import com.anyui.service.SysUserService;
import com.anyui.service.TbPostService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TbPostServiceImpl extends ServiceImpl<TbPostMapper, TbPost> implements TbPostService {

    @Autowired
    private SysUserService userService;

    @Autowired
    private TbPostLikeMapper postLikeMapper;

    /**
     * 根据类别查询帖子 OR 搜索
     * @param category
     * @param keyword
     * @return
     */
    @Override
    public List<PostVO> getPostList(String category, String keyword) {
        // 【核心修改点】
        // 删除了原来 "if (!StringUtils.hasText(category))" 的判断
        // 改为使用 MyBatis-Plus 的动态条件 (condition, column, value)

        List<TbPost> postList = this.lambdaQuery()
                // 1. 如果 category 有值，则拼接: AND category = ?
                .eq(StringUtils.hasText(category), TbPost::getCategory, category)

                // 2. 如果 keyword 有值，则拼接: AND content LIKE %?%
                // 这里的 StringUtils.hasText(keyword) 是控制开关，为 true 时才执行模糊查询
                .like(StringUtils.hasText(keyword), TbPost::getContent, keyword)

                // 3. 排序保持不变
                .orderByDesc(TbPost::getCreateTime)
                .list();

        // 4. 调用公共方法转 VO (保持不变)
        return transferToVOList(postList);
    }

    /**
     * 查询自己发布的帖子
     * @return
     */
    @Override
    public List<PostVO> listMyPosts() {
        // 1. 从 Sa-Token 中获取当前登录用户 ID
        Long loginId = StpUtil.getLoginIdAsLong();

        // 2. 查询该用户的所有帖子，按时间倒序
        List<TbPost> posts = this.list(new LambdaQueryWrapper<TbPost>()
                .eq(TbPost::getUserId, loginId)
                .orderByDesc(TbPost::getCreateTime));

        // 3. 将 TbPost 转换为 PostVO
        return posts.stream().map(post -> {
            PostVO vo = new PostVO();
            BeanUtils.copyProperties(post, vo);

            // 填充发帖人信息 (虽然是自己，但 VO 要求返回)
            SysUser user = userService.getById(loginId);
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar());
            }
            return vo;
        }).collect(Collectors.toList());
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPost(PostAddDTO addDTO) {
        // --- 1. 业务逻辑校验 ---

        // 1.1 内容非空校验
        if (!StringUtils.hasText(addDTO.getContent())) {
            throw new RuntimeException("帖子内容不能为空");
        }

        // 1.2 分类合法性校验
        // 如果前端传了分类，必须是我们在常量类中定义的 5 种之一
        if (StringUtils.hasText(addDTO.getCategory()) && !PostCategoryConstants.isValid(addDTO.getCategory())) {
            throw new RuntimeException("非法的帖子分类类型");
        }

        // --- 2. 数据处理 ---

        // 2.1 获取当前登录用户ID
        long currentUserId = StpUtil.getLoginIdAsLong();

        // 2.2 复制属性 DTO -> Entity
        TbPost post = new TbPost();
        BeanUtils.copyProperties(addDTO, post);

        // 2.3 补全系统字段
        post.setUserId(currentUserId);
        post.setCreateTime(LocalDateTime.now());
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);

        // 2.4 处理默认值
        // 如果前端没传分类，默认为 "campus_life" (校园趣事)
        if (!StringUtils.hasText(post.getCategory())) {
            post.setCategory(PostCategoryConstants.CAMPUS_LIFE);
        }

        // 默认为非匿名 (0)
        if (post.getIsAnonymous() == null) {
            post.setIsAnonymous(0);
        }

        //如果前端没传图片数组，手动设为空列表
        if (post.getMediaUrls() == null) {
            post.setMediaUrls(Collections.emptyList());
        }

        // 3. 保存入库
        this.save(post);
    }

    /**
     * 方法 1: 无参查询 (查全部)
     */
    @Override
    public List<PostVO> getPostList() {
        // 1. 直接查所有，按时间倒序
        List<TbPost> postList = this.lambdaQuery()
                .orderByDesc(TbPost::getCreateTime)
                .list();

        // 2. 调用公共方法转 VO
        return transferToVOList(postList);
    }

    @Override
    public PostVO getPostDetail(Long postId) {
        // 1. 查询帖子
        TbPost post = this.getById(postId);
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }

        // 2. 增加浏览量 (+1)
        post.setViewCount(post.getViewCount() + 1);
        this.updateById(post);

        // 3. 转换 VO
        PostVO vo = new PostVO();
        BeanUtils.copyProperties(post, vo);

        if (vo.getMediaUrls() == null) {
            vo.setMediaUrls(Collections.emptyList());
        }

        // 4. 处理用户信息
        SysUser user = userService.getById(post.getUserId());
        fillUserInfo(vo, post, user);

        // 5. 判断当前登录用户是否点过赞
        if (StpUtil.isLogin()) {
            long currentUserId = StpUtil.getLoginIdAsLong();
            Long count = postLikeMapper.selectCount(new LambdaQueryWrapper<TbPostLike>()
                    .eq(TbPostLike::getUserId, currentUserId)
                    .eq(TbPostLike::getPostId, postId));
            vo.setIsLiked(count > 0);
        } else {
            vo.setIsLiked(false);
        }

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long postId) {
        // 1. 从 Sa-Token 中获取当前登录用户的 ID
        long currentUserId = StpUtil.getLoginIdAsLong();

        // 2. 直接根据 postId 和 userId 尝试删除
        // 这一步在 SQL 层面锁死了权限：只有 ID 匹配且发帖人是当前用户时，才会被删除
        boolean removed = this.remove(new LambdaQueryWrapper<TbPost>()
                .eq(TbPost::getId, postId)
                .eq(TbPost::getUserId, currentUserId));

        // 3. 结果判断
        if (!removed) {
            // 如果没有行受到影响（返回 false），说明帖子不存在或者发生了越权操作
            // 这里抛出异常，触发事务回滚，并由全局异常处理器拦截返回 Result.error
            throw new RuntimeException("删除失败：帖子不存在或您无权操作该内容");
        }
    }

    /**
     * 提取公共方法：填充用户信息
     */
    private void fillUserInfo(PostVO vo, TbPost post, SysUser user) {
        if (post.getIsAnonymous() != null && post.getIsAnonymous() == 1) {
            vo.setNickname("某同学");
            vo.setAvatar("/static/anonymous.png");
        } else {
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatar((user.getAvatar() == null || user.getAvatar().isEmpty())
                        ? "/static/default-avatar.png"
                        : user.getAvatar());
            } else {
                vo.setNickname("未知用户");
                vo.setAvatar("/static/default-avatar.png");
            }
        }
    }

    /**
     * 🔧 提取的公共私有方法：将 List<TbPost> 转换为 List<PostVO>
     * 包含：判空、收集用户ID、查询用户信息、组装VO
     */
    private List<PostVO> transferToVOList(List<TbPost> postList) {
        if (postList.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. 收集所有发帖人的 ID
        List<Long> userIds = postList.stream()
                .map(TbPost::getUserId)
                .distinct()
                .collect(Collectors.toList());

        // 2. 查出用户信息转 Map
        Map<Long, SysUser> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, user -> user));

        // 3. 转换 Entity -> VO
        List<PostVO> voList = new ArrayList<>();
        for (TbPost post : postList) {
            PostVO vo = new PostVO();
            BeanUtils.copyProperties(post, vo);

            // 兜底图片数组
            if (vo.getMediaUrls() == null) {
                vo.setMediaUrls(Collections.emptyList());
            }

            // 填充用户信息 (这里调用了你原本写的 fillUserInfo)
            fillUserInfo(vo, post, userMap.get(post.getUserId()));

            voList.add(vo);
        }
        return voList;
    }
}