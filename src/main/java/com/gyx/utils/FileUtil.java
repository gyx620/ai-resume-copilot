package com.gyx.utils;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * 文件上传工具类
 * <p>
 * 使用 UUID 重命名文件，防止文件名冲突和路径遍历攻击。
 *
 * @author gyx
 */
public class FileUtil {

    /**
     * 保存上传文件到指定目录
     * @param file 上传的文件
     * @param path 存储目录路径
     * @return 新文件名（UUID + 原后缀）
     */
    public static String upload(MultipartFile file, String path) throws IOException {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String originalName = file.getOriginalFilename();
        String suffix = originalName.substring(originalName.lastIndexOf("."));

        // UUID 重命名，避免文件名冲突
        String filename = UUID.randomUUID() + suffix;

        File target = new File(path + filename);
        file.transferTo(target);

        return filename;
    }

}
