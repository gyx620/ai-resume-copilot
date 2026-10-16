package com.gyx.service.impl;

import com.gyx.service.FileService;
import com.gyx.utils.FileUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务实现
 * <p>
 * 面试要点：{@code @Value} 从配置文件注入值，支持默认值语法 {@code ${key:default}}。
 *
 * @author gyx
 */
@Service
public class FileServiceImpl implements FileService {

    @Value("${file.upload-path}")
    private String uploadPath;

    @Override
    public String upload(MultipartFile file) throws Exception {
        String filename = FileUtil.upload(file, uploadPath);
        return "/file/" + filename;
    }

}
