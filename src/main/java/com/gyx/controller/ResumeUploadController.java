package com.gyx.controller;


import com.gyx.common.Result;
import com.gyx.service.ResumeUploadService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;



@RestController
public class ResumeUploadController {

    @Autowired
    private ResumeUploadService resumeUploadService;

    /**
     * 上传简历
     */
    @PostMapping("/api/resume/upload")
    public Result<Long> upload(
            @RequestParam("file") MultipartFile file
    ){

        Long resumeId =
                resumeUploadService.upload(file);


        return Result.success(resumeId);

    }


}