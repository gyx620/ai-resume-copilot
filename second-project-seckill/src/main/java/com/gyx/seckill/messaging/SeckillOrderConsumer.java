package com.gyx.seckill.messaging;

import com.gyx.seckill.config.RabbitMqConfig;
import com.gyx.seckill.exception.BusinessException;
import com.gyx.seckill.service.RedisSeckillStockService;
import com.gyx.seckill.service.SeckillOrderPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class SeckillOrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(SeckillOrderConsumer.class);

    private final SeckillOrderPersistenceService orderPersistenceService;
    private final RedisSeckillStockService stockService;

    public SeckillOrderConsumer(SeckillOrderPersistenceService orderPersistenceService,
                               RedisSeckillStockService stockService) {
        this.orderPersistenceService = orderPersistenceService;
        this.stockService = stockService;
    }

    @RabbitListener(queues = RabbitMqConfig.ORDER_QUEUE)
    public void consume(SeckillOrderMessage message) {
        try {
            orderPersistenceService.createOrder(message);
        } catch (BusinessException e) {
            // 已知业务拒绝不应反复消费；释放本次 Redis 预扣后确认消息。
            stockService.release(message.activityId(), message.userId());
            log.warn("秒杀订单因业务规则未落库，已释放预扣库存。activityId={}, userId={}, reason={}",
                    message.activityId(), message.userId(), e.getMessage());
        }
        // 未知数据库或基础设施异常继续抛出，由 RabbitMQ 保留消息到死信队列，避免静默丢单。
    }
}
