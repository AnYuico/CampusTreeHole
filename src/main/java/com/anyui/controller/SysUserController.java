package com.anyui.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.anyui.common.Result;
import com.anyui.entity.SysUser;
import com.anyui.entity.dto.UserLoginDTO;
import com.anyui.entity.dto.UserRegisterDTO;
import com.anyui.entity.dto.UserUpdateDTO;
import com.anyui.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "用户管理模块")
@RestController
@RequestMapping("/sysUser")
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;


    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<String> register(@RequestBody UserRegisterDTO registerDTO) {
        // 所有的校验和逻辑都在 Service 里的 register 方法中
        // 如果出错，Service 会抛异常，GlobalExceptionHandler 会捕获并返回错误信息
        sysUserService.register(registerDTO);
        return Result.success("注册成功");
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody UserLoginDTO loginDTO) {
        // 注意：这里入参建议改为 UserLoginDTO，不要直接用 SysUser，保持规范
        Map<String, Object> map = sysUserService.login(loginDTO);
        return Result.success(map);
    }

    @Operation(summary = "用户信息修改")
    @PostMapping("/update")
    public Result<String> update(@RequestBody UserUpdateDTO updateDTO) {
        // 不需要在这里获取 Token ID 了，Service 会自己获取
        sysUserService.updateUserInfo(updateDTO);
        return Result.success("修改成功");
    }

    @Operation(summary = "用户退出登录")
    @PostMapping("/logout")
    public Result<String> logout() {
        sysUserService.logout();
        return Result.success("退出成功");
    }

    @Operation(summary = "修改个人头像")
    @PostMapping("/updateAvatar")
    public Result<String> updateAvatar(@RequestParam String avatarUrl) {
        // 1. 获取当前登录用户 ID
        long userId = StpUtil.getLoginIdAsLong();

        // 2. 更新数据库
        SysUser user = new SysUser();
        user.setId(userId);
        user.setAvatar(avatarUrl);

        boolean success = sysUserService.updateById(user);

        if (success) {
            return Result.success("头像修改成功");
        } else {
            return Result.error("修改失败");
        }
    }

    /**
     * 获取当前登录用户的信息
     */
    @Operation(summary = "获取登录用户的信息")
    @GetMapping("/info")
    public Result<SysUser> info() {
        // 1. C层职责：解析请求上下文 (从 Token 拿 ID)
        long userId = StpUtil.getLoginIdAsLong();

        // 2. C层职责：调用 S 层接口
        SysUser userInfo = sysUserService.getUserInfo(userId);

        // 3. C层职责：包装统一响应格式
        return Result.success(userInfo);
    }
}