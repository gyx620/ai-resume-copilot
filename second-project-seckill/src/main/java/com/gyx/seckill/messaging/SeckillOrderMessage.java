package com.gyx.seckill.messaging;

import java.io.Serializable;
import java.math.BigDecimal;

public record SeckillOrderMessage(Long activityId,
                                  Long userId,
                                  Long productId,
                                  BigDecimal seckillPrice) implements Serializable {
}
