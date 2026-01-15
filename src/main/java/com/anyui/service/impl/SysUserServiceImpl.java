package com.anyui.service.impl;

import cn.dev33.satoken.secure.SaSecureUtil;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.anyui.entity.SysUser;
import com.anyui.entity.dto.UserLoginDTO;
import com.anyui.entity.dto.UserRegisterDTO;
import com.anyui.entity.dto.UserUpdateDTO;
import com.anyui.mapper.SysUserMapper;
import com.anyui.service.SysUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Override
    public SysUser getUserInfo(Long userId) {
        // 1. 查库 (MyBatis-Plus 提供的 getById)
        SysUser user = this.getById(userId);

        // 2. 业务处理：脱敏 (C层不应该干这种脏活，S层处理完再给C层)
        if (user != null) {
            user.setPassword(null);
            // 如果还有其他敏感信息比如手机号中间四位隐藏，也在这里做
        }

        // 3. 返回处理好的数据
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(UserRegisterDTO registerDTO) {
        // 1. 校验用户名是否已存在
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, registerDTO.getUsername());
        if (this.count(wrapper) > 0) {
            throw new RuntimeException("用户名已存在，请更换！");
        }

        // 2. 校验两次密码是否一致
        if (!registerDTO.getPassword().equals(registerDTO.getCheckPassword())) {
            throw new RuntimeException("两次输入的密码不一致！");
        }

        // 3. 封装用户对象
        SysUser user = new SysUser();
        user.setUsername(registerDTO.getUsername());

        // 4. 密码加密 (统一使用 SaSecureUtil)
        String md5Password = SaSecureUtil.md5(registerDTO.getPassword());
        user.setPassword(md5Password);

        // 5. 设置昵称和性别
        if (registerDTO.getNickname() != null && !registerDTO.getNickname().isEmpty()) {
            user.setNickname(registerDTO.getNickname());
        } else {
            user.setNickname("用户_" + registerDTO.getUsername());
        }

        user.setGender(registerDTO.getGender() != null ? registerDTO.getGender() : 0);
        user.setCreateTime(LocalDateTime.now()); // 补充注册时间

        // 6. 保存
        this.save(user);
    }

    @Override
    public Map<String, Object> login(UserLoginDTO loginDTO) {
        // 1. 根据用户名查询
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, loginDTO.getUsername());
        // 直接匹配加密后的密码
        wrapper.eq(SysUser::getPassword, SaSecureUtil.md5(loginDTO.getPassword()));

        SysUser user = this.getOne(wrapper);

        // 2. 判断结果
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 3. Sa-Token 登录 (颁发 Token)
        StpUtil.login(user.getId());

        // 4. 获取 Token 信息
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();

        // 5. 组装返回结果
        Map<String, Object> map = new HashMap<>();
        map.put("tokenName", tokenInfo.tokenName);
        map.put("tokenValue", tokenInfo.tokenValue);

        // 脱敏处理：不把密码返回给前端
        user.setPassword(null);
        map.put("userInfo", user);

        return map;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserInfo(UserUpdateDTO updateDTO) {
        // 1. 获取当前登录用户的 ID (从 Token 获取，安全可靠)
        long currentUserId = StpUtil.getLoginIdAsLong();

        // 2. 查询用户
        SysUser user = this.getById(currentUserId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 3. 更新字段
        if (updateDTO.getNickname() != null) user.setNickname(updateDTO.getNickname());
        if (updateDTO.getAvatar() != null) user.setAvatar(updateDTO.getAvatar());
        if (updateDTO.getGender() != null) user.setGender(updateDTO.getGender());

        // 4. 执行更新
        this.updateById(user);
    }

    @Override
    public void logout() {
        // Sa-Token 注销当前会话
        // 这会让当前的 Token 立即失效，再次访问其他接口会报 401
        StpUtil.logout();
    }

    @Override
    public void updateUserAvatar(String avatarUrl) {
        // 1. 获取当前登录用户ID
        long userId = StpUtil.getLoginIdAsLong();

        // 2. 构建更新对象 (只更新 avatar 字段，其他不动)
        SysUser updateWrapper = new SysUser();
        updateWrapper.setId(userId);
        updateWrapper.setAvatar(avatarUrl);

        // 3. 执行更新
        boolean success = this.updateById(updateWrapper);
        if (!success) {
            throw new RuntimeException("头像更新失败");
        }
    }

}