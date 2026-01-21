package com.anyui.service.impl;

import com.anyui.entity.SysConfig;
import com.anyui.mapper.SysConfigMapper;
import com.anyui.service.SysConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {

    private static final String KEY_AI_AUDIT = "ai_audit_enabled";

    @Override
    public boolean isAiAuditEnabled() {
        // 1. 查询数据库
        SysConfig config = this.getOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getParamKey, KEY_AI_AUDIT));

        // 2. 如果没配，默认算开启 (根据你之前的逻辑)
        return config != null && "true".equalsIgnoreCase(config.getParamValue());

        // 💡 优化建议：如果访问量大，建议这里加 Redis 缓存，不要每次都查库
        // String val = redisTemplate.opsForValue().get("config:" + KEY_AI_AUDIT);
        // return "true".equals(val);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setAiAuditEnabled(Boolean open) {
        // 1. 查询是否存在
        SysConfig config = this.getOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getParamKey, KEY_AI_AUDIT));

        // 2. 如果不存在则新建
        if (config == null) {
            config = new SysConfig();
            config.setParamKey(KEY_AI_AUDIT);
            config.setRemark("AI审核开关");
        }

        // 3. 更新值
        config.setParamValue(String.valueOf(open));

        // 4. 保存或更新 (MyBatis-Plus 提供的 saveOrUpdate 方法)
        this.saveOrUpdate(config);

        // 💡 优化建议：如果有 Redis，记得在这里同步更新/删除缓存
        // redisTemplate.opsForValue().set("config:" + KEY_AI_AUDIT, String.valueOf(open));
    }
}