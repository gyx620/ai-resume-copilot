package com.gyx.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务接口
 *
 * @author gyx
 */
public interface FileService {

    /** 上传文件，返回可访问的 URL 路径 */
    String upload(MultipartFile file) throws Exception;

}
