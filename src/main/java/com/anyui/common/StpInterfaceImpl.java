package com.anyui.common;

import cn.dev33.satoken.stp.StpInterface;
import com.anyui.entity.SysUser;
import com.anyui.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 自定义权限验证接口扩展
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Autowired
    private SysUserService sysUserService;

    /**
     * 返回一个账号所拥有的权限码集合 (目前项目简单，可以先返回空)
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return new ArrayList<>();
    }

    /**
     * 返回一个账号所拥有的角色标识集合 (权限验证的核心)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 1. 根据 loginId 查询用户信息
        SysUser user = sysUserService.getById(Long.parseLong(loginId.toString()));

        // 2. 如果用户存在，返回他的角色
        if (user != null && user.getRole() != null) {
            return Collections.singletonList(user.getRole());
        }

        // 3. 默认返回空
        return new ArrayList<>();
    }
}