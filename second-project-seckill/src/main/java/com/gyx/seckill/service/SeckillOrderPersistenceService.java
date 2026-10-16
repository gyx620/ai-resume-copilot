package com.gyx.seckill.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gyx.seckill.entity.Product;
import com.gyx.seckill.entity.SeckillOrder;
import com.gyx.seckill.exception.BusinessException;
import com.gyx.seckill.mapper.ProductMapper;
import com.gyx.seckill.mapper.SeckillActivityMapper;
import com.gyx.seckill.mapper.SeckillOrderMapper;
import com.gyx.seckill.messaging.SeckillOrderMessage;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SeckillOrderPersistenceService {

    private final ProductMapper productMapper;
    private final SeckillActivityMapper activityMapper;
    private final SeckillOrderMapper orderMapper;

    public SeckillOrderPersistenceService(ProductMapper productMapper,
                                          SeckillActivityMapper activityMapper,
                                          SeckillOrderMapper orderMapper) {
        this.productMapper = productMapper;
        this.activityMapper = activityMapper;
        this.orderMapper = orderMapper;
    }

    @Transactional
    public SeckillOrder createOrder(SeckillOrderMessage message) {
        Long activityId = message.activityId();
        Long userId = message.userId();
        SeckillOrder existingOrder = orderMapper.selectOne(new LambdaQueryWrapper<SeckillOrder>()
                .select(SeckillOrder::getId)
                .eq(SeckillOrder::getUserId, userId)
                .eq(SeckillOrder::getActivityId, activityId));
        if (existingOrder != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "你已经参加过本次秒杀");
        }

        Product product = productMapper.selectById(message.productId());
        if (product == null || product.getStatus() != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "商品当前不可购买");
        }

        int updatedRows = activityMapper.decreaseStockIfAvailable(activityId);
        if (updatedRows == 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "活动已结束、已停用或库存已售罄");
        }

        SeckillOrder order = new SeckillOrder();
        order.setOrderNo(UUID.randomUUID().toString().replace("-", ""));
        order.setUserId(userId);
        order.setActivityId(activityId);
        order.setProductId(message.productId());
        order.setQuantity(1);
        order.setUnitPrice(message.seckillPrice());
        order.setTotalAmount(message.seckillPrice());
        order.setStatus(0);
        orderMapper.insert(order);
        return order;
    }

    public SeckillOrder findOrder(Long activityId, Long userId) {
        return orderMapper.selectOne(new LambdaQueryWrapper<SeckillOrder>()
                .eq(SeckillOrder::getActivityId, activityId)
                .eq(SeckillOrder::getUserId, userId));
    }
}
