package com.gyx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("resume_analysis")
public class ResumeAnalysis {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的简历 ID */
    private Long resumeId;

    /** 所属用户 ID */
    private Long userId;

    /** 分析结果 JSON 字符串 */
    private String result;

    /** 分析时间 */
    private LocalDateTime createTime;

}
