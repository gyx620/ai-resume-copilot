package com.gyx.seckill.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ActivityView {
    private Long activityId;
    private Long productId;
    private String productName;
    private BigDecimal originalPrice;
    private BigDecimal seckillPrice;
    private Integer stock;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
