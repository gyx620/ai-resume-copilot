package com.gyx.controller;

import com.gyx.common.Result;
import com.gyx.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传接口
 * <p>
 * 面试要点：{@code @RequestParam("file")} 绑定表单文件字段，
 * {@link MultipartFile} 是 Spring 对上传文件的抽象，提供 {@code transferTo()} 等方法。
 *
 * @author gyx
 */
@RestController
@RequestMapping("/api/file")
public class FileController {

    @Autowired
    private FileService fileService;

    /** POST /api/file/upload —— 上传文件，返回访问 URL */
    @PostMapping("/upload")
    public Result<?> upload(@RequestParam("file") MultipartFile file) throws Exception {
        String url = fileService.upload(file);
        return Result.success(url);
    }

}
