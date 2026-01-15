package com.anyui.controller;

import com.anyui.common.Result;
import com.anyui.common.utils.MinioUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "通用功能接口")
@RestController
@RequestMapping("/common")
public class CommonController {

    @Autowired
    private MinioUtil minioUtil;

    @Operation(summary = "文件上传")
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public Result<String> upload(@RequestPart("file") MultipartFile file) {
        // 统一把所有图片都传到 "images" 目录下
        // 如果你想区分头像和帖子，可以让前端多传一个 type 参数，这里暂且简单处理
        String url = minioUtil.upload(file, "images");
        return Result.success(url);
    }
}