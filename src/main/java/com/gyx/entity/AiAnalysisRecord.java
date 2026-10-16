package com.gyx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_analysis_record")
public class AiAnalysisRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long resumeId;

    private Integer score;

    private String advantage;

    private String problem;

    private String suggestion;

    private LocalDateTime createTime;
}
