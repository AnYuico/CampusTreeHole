package com.anyui.service;

import com.anyui.entity.SysUser;
import com.anyui.entity.dto.UserLoginDTO;
import com.anyui.entity.dto.UserRegisterDTO;
import com.anyui.entity.dto.UserUpdateDTO;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.Map;

public interface SysUserService extends IService<SysUser> {

    /**
     * 用户注册
     */
    void register(UserRegisterDTO registerDTO);

    /**
     * 用户登录
     * @return 包含 Token 和用户信息的 Map
     */
    Map<String, Object> login(UserLoginDTO loginDTO);

    /**
     * 修改用户信息 (当前登录用户)
     */
    void updateUserInfo(UserUpdateDTO updateDTO);

    /**
     * 用户退出登录
     */
    void logout();

    /**
     * 修改当前登录用户的头像
     * @param avatarUrl 图片地址
     */
    void updateUserAvatar(String avatarUrl);


    /**
     * 获取个人信息
      * @param userId
     * @return
     */
    SysUser getUserInfo(Long userId);
}