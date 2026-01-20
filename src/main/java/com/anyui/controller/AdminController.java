package com.anyui.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.anyui.common.Result;
import com.anyui.entity.SysConfig;
import com.anyui.entity.SysUser;
import com.anyui.entity.dto.PostAuditDTO;
import com.anyui.entity.vo.PostVO;
import com.anyui.mapper.SysConfigMapper;
import com.anyui.service.SysUserService;
import com.anyui.service.TbPostService;
import com.anyui.service.impl.TbPostServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@Tag(name = "管理员后台模块")
@SaCheckRole("admin")
public class AdminController {

    @Autowired
    private TbPostService postService;

    @Autowired
    private SysUserService userService;

    @Autowired
    private SysConfigMapper sysConfigMapper;

    // ==================== 1. 帖子审核相关 ====================

    @Operation(summary = "获取待审核帖子列表")
    @GetMapping("/audit/list")
    public Result<List<PostVO>> getPendingPosts() {
        // C层只管调用，逻辑去S层找
        return Result.success(postService.getPendingPosts());
    }

    @Operation(summary = "执行审核操作")
    @PostMapping("/audit/do")
    // ✅ 修改点：使用 @RequestBody 接收 JSON 对象
    public Result<String> auditPost(@RequestBody PostAuditDTO auditDTO) {
        // 调用 Service (Service 层保持不变，还是接收 3 个参数，这样耦合度低)
        postService.auditPost(
                auditDTO.getPostId(),
                auditDTO.getPass(),
                auditDTO.getReason()
        );
        return Result.success("审核操作完成");
    }

    // ==================== 2. 全局帖子管理 ====================

    @Operation(summary = "管理员删除帖子(强制删除)")
    @DeleteMapping("/post/delete")
    public Result<String> forceDeletePost(@RequestParam Long postId) {
        postService.forceDeletePost(postId);
        return Result.success("已强制删除");
    }

    // ==================== 3. 用户管理相关 ====================

    @Operation(summary = "获取所有用户列表")
    @GetMapping("/user/list")
    public Result<List<SysUser>> getUserList(@RequestParam(defaultValue = "") String keyword) {
        return Result.success(userService.searchUsers(keyword));
    }

    @Operation(summary = "重置用户头像/昵称 (违规处理)")
    @PostMapping("/user/reset")
    public Result<String> resetUserInfo(@RequestParam Long userId) {
        userService.resetUser(userId);
        return Result.success("用户信息已重置");
    }

    // ==================== 4. 数据统计 ====================

    @Operation(summary = "获取后台首页统计数据")
    @GetMapping("/stats")
    public Result<Map<String, Object>> getDashboardStats() {
        // 这个逻辑稍微复杂，我们可以放在 PostService 或者专门的 AdminService
        // 这里暂时放在 PostService 里
        return Result.success(postService.getDashboardStats());
    }

    // ==================== 5. 系统配置管理 (AI开关) ====================

    @Operation(summary = "获取AI审核开关状态")
    @GetMapping("/config/ai-status")
    public Result<Boolean> getAiAuditStatus() {
        SysConfig config = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getParamKey, "ai_audit_enabled"));

        // 如果没配，默认算开启
        boolean isOpen = config != null && "true".equalsIgnoreCase(config.getParamValue());
        return Result.success(isOpen);
    }

    @Operation(summary = "修改AI审核开关")
    @PostMapping("/config/ai-status")
    public Result<String> updateAiAuditStatus(@RequestParam Boolean open) {
        // 1. 数据库操作 (保持原样)
        SysConfig config = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getParamKey, "ai_audit_enabled"));

        if (config == null) {
            config = new SysConfig();
            config.setParamKey("ai_audit_enabled");
            config.setRemark("AI审核开关");
        }

        config.setParamValue(String.valueOf(open));

        if (config.getId() == null) {
            sysConfigMapper.insert(config);
        } else {
            sysConfigMapper.updateById(config);
        }

        // ✅ 核心修改：手动更新 Service 里的缓存
        // 这样下一次发帖时，checkAiSwitch() 拿到的就是最新值
        TbPostServiceImpl.AI_SWITCH_CACHE = open;

        return Result.success("设置成功，当前状态：" + (open ? "开启" : "关闭"));
    }
}