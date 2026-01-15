package com.anyui.common.utils;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Component
public class MinioUtil {

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String endpoint;

    /**
     * 上传文件
     * @param file 前端传来的文件对象
     * @param dir  保存的目录 (例如: "avatar", "post")
     * @return 文件的完整访问 URL
     */
    public String upload(MultipartFile file, String dir) {
        try {
            // 1. 获取原文件名后缀 (例如 .jpg)
            String originalFilename = file.getOriginalFilename();
            String suffix = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // 2. 生成新文件名: 目录/UUID.后缀
            // 例如: avatar/550e8400-e29b-41d4-a716-446655440000.jpg
            String objectName = dir + "/" + UUID.randomUUID().toString() + suffix;

            // 3. 执行上传
            InputStream inputStream = file.getInputStream();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            // 4. 拼接返回 URL
            // 格式: http://1.2.3.4:9000/anyui-oss/avatar/xxx.jpg
            return endpoint + "/" + bucketName + "/" + objectName;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("图片上传失败: " + e.getMessage());
        }
    }
}