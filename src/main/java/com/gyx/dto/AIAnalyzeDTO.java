package com.gyx.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AIAnalyzeDTO {

    @NotNull(message = "简历ID不能为空")
    private Long resumeId;
}
