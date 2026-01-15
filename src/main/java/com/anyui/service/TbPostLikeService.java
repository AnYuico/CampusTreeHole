package com.anyui.service;

import com.anyui.entity.TbPostLike;
import com.anyui.entity.dto.PostLikeDTO;
import com.baomidou.mybatisplus.extension.service.IService;

public interface TbPostLikeService extends IService<TbPostLike> {

    /**
     * 点赞 / 取消点赞 (自动切换)
     * @param likeDTO 包含 postId
     */
    void toggleLike(PostLikeDTO likeDTO);
}