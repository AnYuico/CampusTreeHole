package com.anyui.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson2.JSON;
import com.anyui.common.PostCategoryConstants;
import com.anyui.entity.SysConfig;
import com.anyui.entity.SysUser;
import com.anyui.entity.TbPost;
import com.anyui.entity.TbPostLike;
import com.anyui.entity.dto.AuditResult;
import com.anyui.entity.dto.PostAddDTO;
import com.anyui.entity.dto.PostDTO;
import com.anyui.entity.vo.PostVO;
import com.anyui.mapper.SysConfigMapper;
import com.anyui.mapper.TbPostLikeMapper;
import com.anyui.mapper.TbPostMapper;
import com.anyui.service.AiAuditService;
import com.anyui.service.SysUserService;
import com.anyui.service.TbPostService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.anyui.common.PostStatusEnum;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TbPostServiceImpl extends ServiceImpl<TbPostMapper, TbPost> implements TbPostService {

    @Autowired
    private SysUserService userService;

    @Autowired
    private TbPostLikeMapper postLikeMapper;

    @Autowired
    private AiAuditService aiAuditService;

    @Autowired
    private SysConfigMapper sysConfigMapper;


    // ✅ 新增：定义一个全局静态缓存 (volatile 保证多线程可见性)
    // 初始值为 null，表示还没从数据库读过
    public static volatile Boolean AI_SWITCH_CACHE = null;

    /**
     * 根据类别查询帖子 OR 搜索 (公共列表)
     * 修改点：增加 status = 1 的过滤条件
     */
    @Override
    public List<PostVO> getPostList(String category, String keyword) {
        List<TbPost> postList = this.lambdaQuery()
                // ✅ 核心修改：只查询状态为 1 (审核通过) 的帖子
                .eq(TbPost::getStatus, PostStatusEnum.APPROVED.getCode())

                // 1. 如果 category 有值，则拼接: AND category = ?
                .eq(StringUtils.hasText(category), TbPost::getCategory, category)

                // 2. 如果 keyword 有值，则拼接: AND content LIKE %?%
                .like(StringUtils.hasText(keyword), TbPost::getContent, keyword)

                // 3. 排序
                .orderByDesc(TbPost::getCreateTime)
                .list();

        // 4. 调用公共方法转 VO
        return transferToVOList(postList);
    }

    /**
     * 方法 1: 无参查询 (公共列表 - 查全部)
     * 修改点：增加 status = 1 的过滤条件
     */
    @Override
    public List<PostVO> getPostList() {
        List<TbPost> postList = this.lambdaQuery()
                // ✅ 核心修改：只查询状态为 1 (审核通过) 的帖子
                .eq(TbPost::getStatus, PostStatusEnum.APPROVED.getCode())

                .orderByDesc(TbPost::getCreateTime)
                .list();

        return transferToVOList(postList);
    }

    /**
     * 查询自己发布的帖子
     * 保持不变：返回所有状态的帖子，供前端展示不同标签（审核中/已拒绝）
     */
    @Override
    public List<PostVO> listMyPosts() {
        // 1. 从 Sa-Token 中获取当前登录用户 ID
        Long loginId = StpUtil.getLoginIdAsLong();

        // 2. 查询该用户的所有帖子 (不加 status 过滤)
        List<TbPost> posts = this.list(new LambdaQueryWrapper<TbPost>()
                .eq(TbPost::getUserId, loginId)
                .orderByDesc(TbPost::getCreateTime));

        // 3. 转换为 VO
        return posts.stream().map(post -> {
            PostVO vo = new PostVO();
            BeanUtils.copyProperties(post, vo);

            // 填充用户信息
            SysUser user = userService.getById(loginId);
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * Admin: 获取待审核帖子
     */
    @Override
    public List<PostVO> getPendingPosts() {
        List<TbPost> list = this.lambdaQuery()
                .eq(TbPost::getStatus, PostStatusEnum.PENDING.getCode())
                .orderByDesc(TbPost::getCreateTime)
                .list();
        // 复用之前的转换逻辑
        return transferToVOList(list);
    }

    /**
     * Admin: 审核帖子
     */
    @Override
    public void auditPost(Long postId, Boolean pass, String reason) {
        if (postId == null || pass == null) {
            throw new RuntimeException("参数错误");
        }

        LambdaUpdateWrapper<TbPost> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(TbPost::getId, postId);

        if (pass) {
            updateWrapper.set(TbPost::getStatus, PostStatusEnum.APPROVED.getCode());
        } else {
            updateWrapper.set(TbPost::getStatus, PostStatusEnum.REJECTED.getCode());
            String finalReason = StringUtils.hasText(reason) ? reason : "内容不符合社区规范";
            updateWrapper.set(TbPost::getReason, finalReason);
        }

        boolean update = this.update(updateWrapper);
        if (!update) {
            throw new RuntimeException("操作失败，帖子不存在");
        }
    }

    /**
     * Admin: 强制删除
     */
    @Override
    public void forceDeletePost(Long postId) {
        boolean removed = this.removeById(postId);
        if (!removed) {
            throw new RuntimeException("删除失败，数据可能已不存在");
        }
    }

    /**
     * Admin: 仪表盘统计
     */
    @Override
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        long userCount = userService.count();
        long postCount = this.count();
        long pendingCount = this.lambdaQuery()
                .eq(TbPost::getStatus, PostStatusEnum.PENDING.getCode())
                .count();

        stats.put("userCount", userCount);
        stats.put("postCount", postCount);
        stats.put("pendingCount", pendingCount);

        return stats;
    }

    // ==================== 1. 发布帖子 ====================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPost(PostAddDTO addDTO) {
        TbPost post = new TbPost();
        BeanUtils.copyProperties(addDTO, post);

        long userId = StpUtil.getLoginIdAsLong();
        post.setUserId(userId);
        post.setCreateTime(LocalDateTime.now());
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);

        // 处理图片 (TypeHandler 会自动处理 List<String>)
        if (addDTO.getMediaUrls() == null) {
            post.setMediaUrls(Collections.emptyList());
        }

        // ✅ 调用抽取的审核逻辑
        this.processAiAudit(post);

        this.save(post);
    }

    // ==================== 2. 修改帖子 ====================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePost(PostDTO updateDTO) {
        // 1. 查
        TbPost post = this.getById(updateDTO.getId());
        if (post == null) throw new RuntimeException("帖子不存在");

        // 2. 权
        long currentUserId = StpUtil.getLoginIdAsLong();
        if (!post.getUserId().equals(currentUserId)) {
            throw new RuntimeException("无权修改他人帖子");
        }

        // 3. 改
        post.setContent(updateDTO.getContent());
        post.setCategory(updateDTO.getCategory());
        post.setIsAnonymous(updateDTO.getIsAnonymous());

        // 处理图片
        if (updateDTO.getMediaUrls() != null) {
            post.setMediaUrls(updateDTO.getMediaUrls());
        } else {
            post.setMediaUrls(Collections.emptyList());
        }

        // ✅ 调用抽取的审核逻辑 (核心解耦点)
        // 只要内容变了，就无脑重审，逻辑统一
        this.processAiAudit(post);

        // 4. 存
        this.updateById(post);
    }

    // ==================== ⬇️ 核心抽取：统一审核逻辑 ⬇️ ====================
    /**
     * 执行AI审核并设置帖子状态
     * @param post 待保存/更新的帖子对象
     */
    private void processAiAudit(TbPost post) {
        try {
            // 调用 AI 服务
            AuditResult result = aiAuditService.auditText(post.getContent());

            if (result.isPass()) {
                post.setStatus(1); // 通过
                post.setReason("");
            } else {
                post.setStatus(2); // 拒绝
                post.setReason(result.getReason());
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 🛑 服务降级逻辑
            // AI 挂了 -> 转人工审核 (Status=0)
            post.setStatus(0);
            post.setReason("AI服务繁忙，等待人工审核");
        }
    }



    /**
     * ✅ 修改后的检查方法：优先读缓存
     */
    private boolean checkAiSwitch() {
        // 1. 如果缓存不为空，直接返回缓存值 (不查库，极快)
        if (AI_SWITCH_CACHE != null) {
            return AI_SWITCH_CACHE;
        }

        // 2. 缓存为空，查数据库
        SysConfig config = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getParamKey, "ai_audit_enabled"));

        boolean isOpen = config != null && "true".equalsIgnoreCase(config.getParamValue());

        // 3. 将结果写入缓存，下次就不用查库了
        AI_SWITCH_CACHE = isOpen;

        return isOpen;
    }

    @Override
    public PostVO getPostDetail(Long postId) {
        // 1. 查询帖子
        TbPost post = this.getById(postId);
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }

        // ✅ 安全增强建议：
        // 如果帖子未过审 (status != 1) 且 当前用户不是作者 且 当前用户不是管理员
        // 则抛出异常，禁止访问
        long currentUserId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : -1;
        boolean isAuthor = post.getUserId().equals(currentUserId);
        boolean isAdmin = StpUtil.hasRole("admin"); // 假设你配置了 admin 角色

        if (!PostStatusEnum.APPROVED.getCode().equals(post.getStatus()) && !isAuthor && !isAdmin) {
            throw new RuntimeException("该内容正在审核中或已被隐藏，无法查看");
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