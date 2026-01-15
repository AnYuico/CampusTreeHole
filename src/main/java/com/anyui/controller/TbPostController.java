package com.anyui.controller;

import cn.dev33.satoken.annotation.SaCheckLogin; // ✅ 新增：Sa-Token 鉴权注解
import cn.dev33.satoken.annotation.SaCheckRole;
import com.anyui.common.Result;
import com.anyui.entity.dto.PostAddDTO;
import com.anyui.entity.vo.PostVO;
import com.anyui.service.TbPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.ibatis.io.ResolverUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "帖子管理模块")
@RestController
@RequestMapping("/post")
public class TbPostController {

    @Autowired
    private TbPostService postService;

    @Operation(summary = "发布帖子")
    @SaCheckLogin // 标记此接口需要登录
    @PostMapping("/add")
    public Result<String> add(@RequestBody PostAddDTO addDTO) {
        // 所有的判空、Token获取、默认值设置都在 Service 里
        postService.addPost(addDTO);
        return Result.success("发布成功");
    }

    @Operation(summary = "获取帖子列表")
    @GetMapping("/list")
    // ✅ 修改：接收 category 参数，非必填
    public Result<List<PostVO>> list(@RequestParam(required = false) String category) {
        // 将参数传给 Service
        List<PostVO> list = postService.getPostList(category);
        return Result.success(list);
    }

    @Operation(summary = "删除帖子")
    @SaCheckLogin // 标记此接口需要登录
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam Long postId) {
        // 所有的权限校验、数据库删除都在 Service 里
        postService.deletePost(postId);
        return Result.success("删除成功");
    }

    @Operation(summary = "获取帖子详情")
    @GetMapping("/detail")
    public Result<PostVO> detail(@RequestParam Long postId) {
        PostVO vo = postService.getPostDetail(postId);
        return Result.success(vo);
    }

    @Operation(summary = "获取当前登录用户的树洞列表")
    @GetMapping("/listMyPosts")
    public Result<List<PostVO>> listMyPosts() {
        // 直接调用 Service 层逻辑
        return Result.success(postService.listMyPosts());
    }


}