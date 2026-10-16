package com.gyx.seckill.dto;

import com.gyx.seckill.entity.SeckillOrder;

public record SeckillOrderStatusView(String status, SeckillOrder order) {
}
