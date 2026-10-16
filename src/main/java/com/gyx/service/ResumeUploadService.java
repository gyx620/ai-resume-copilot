package com.gyx.service;


import org.springframework.web.multipart.MultipartFile;


public interface ResumeUploadService {


    Long upload(MultipartFile file);


}