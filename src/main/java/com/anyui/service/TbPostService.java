package com.anyui.service;

import com.anyui.entity.TbPost;
import com.anyui.entity.dto.PostAddDTO;
import com.anyui.entity.vo.PostVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface TbPostService extends IService<TbPost> {

    /**
     * 发布帖子
     * @param addDTO 发布参数
     */
    void addPost(PostAddDTO addDTO);

    /**
     * 获取帖子列表 (包含用户信息处理)
     * @return VO列表
     */
    List<PostVO> getPostList();

    /**
     * 删除帖子
     * @param postId 帖子ID
     */
    void deletePost(Long postId);

    /**
     * 获取帖子详情
     * @param postId
     * @return
     */
    public PostVO getPostDetail(Long postId);

    /**
     * 根据类型分类帖子
     * @param category
     * @return
     */
    List<PostVO> getPostList(String category,String keyword);

    /**
     * 查询自己的帖子
     * @return
     */
    List<PostVO> listMyPosts();
}