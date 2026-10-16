package com.gyx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("resume_record")
public class ResumeRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    @TableField("file_name")
    private String fileName;

    @TableField("file_url")
    private String fileUrl;

    private String content;

    @TableField("ai_score")
    private Integer aiScore;

    @TableField("ai_result")
    private String aiResult;

    @TableField("create_time")
    private LocalDateTime createTime;
}
