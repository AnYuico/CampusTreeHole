package com.anyui.service;

import com.anyui.entity.SysConfig;
import com.baomidou.mybatisplus.extension.service.IService;

public interface SysConfigService extends IService<SysConfig> {

    /**
     * 获取AI审核是否开启
     * @return true=开启, false=关闭
     */
    boolean isAiAuditEnabled();

    /**
     * 设置AI审核开关
     * @param open true=开启, false=关闭
     */
    void setAiAuditEnabled(Boolean open);
}