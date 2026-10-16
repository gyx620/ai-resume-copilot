package com.gyx.dto;

import lombok.Data;

@Data
public class ResumeAnalyzeResultDTO {

    private Integer score;

    private String advantage;

    private String problem;

    private String suggestion;
}