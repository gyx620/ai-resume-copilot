package com.gyx.seckill.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gyx.seckill.dto.ActivityView;
import com.gyx.seckill.dto.SeckillOrderStatusView;
import com.gyx.seckill.dto.SeckillRequestAccepted;
import com.gyx.seckill.entity.Product;
import com.gyx.seckill.entity.SeckillActivity;
import com.gyx.seckill.entity.SeckillOrder;
import com.gyx.seckill.exception.BusinessException;
import com.gyx.seckill.mapper.ProductMapper;
import com.gyx.seckill.mapper.SeckillActivityMapper;
import com.gyx.seckill.messaging.SeckillOrderMessage;
import com.gyx.seckill.messaging.SeckillOrderPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SeckillService {

    private static final Logger log = LoggerFactory.getLogger(SeckillService.class);

    private final ProductMapper productMapper;
    private final SeckillActivityMapper activityMapper;
    private final RedisSeckillStockService stockService;
    private final SeckillOrderPublisher orderPublisher;
    private final SeckillOrderPersistenceService orderPersistenceService;

    public SeckillService(ProductMapper productMapper,
                          SeckillActivityMapper activityMapper,
                          RedisSeckillStockService stockService,
                          SeckillOrderPublisher orderPublisher,
                          SeckillOrderPersistenceService orderPersistenceService) {
        this.productMapper = productMapper;
        this.activityMapper = activityMapper;
        this.stockService = stockService;
        this.orderPublisher = orderPublisher;
        this.orderPersistenceService = orderPersistenceService;
    }

    public List<ActivityView> listActivities() {
        return activityMapper.selectList(new LambdaQueryWrapper<SeckillActivity>()
                        .eq(SeckillActivity::getStatus, 1)
                        .orderByAsc(SeckillActivity::getStartTime))
                .stream()
                .map(this::toActivityView)
                .toList();
    }

    public SeckillRequestAccepted createOrder(Long activityId, Long userId) {
        SeckillActivity activity = activityMapper.selectById(activityId);
        if (activity == null || activity.getStatus() != 1) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "秒杀活动不存在或已停用");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime())) {
            throw new BusinessException(HttpStatus.CONFLICT, "秒杀活动尚未开始");
        }
        if (now.isAfter(activity.getEndTime())) {
            throw new BusinessException(HttpStatus.CONFLICT, "秒杀活动已结束");
        }

        Product product = productMapper.selectById(activity.getProductId());
        if (product == null || product.getStatus() != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "商品当前不可购买");
        }

        long reservationResult = stockService.reserve(activityId, userId);
        if (reservationResult == -1) {
            throw new BusinessException(HttpStatus.CONFLICT, "你已经参加过本次秒杀");
        }
        if (reservationResult == 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "秒杀商品已售罄");
        }
        if (reservationResult == -2) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "秒杀库存尚未加载，请稍后重试");
        }
        if (reservationResult != 1) {
            throw new IllegalStateException("Redis 秒杀脚本返回了未知状态");
        }

        try {
            orderPublisher.publish(new SeckillOrderMessage(
                    activityId, userId, product.getId(), activity.getSeckillPrice()));
            return new SeckillRequestAccepted(activityId, userId, "PROCESSING");
        } catch (RuntimeException e) {
            try {
                stockService.release(activityId, userId);
            } catch (RuntimeException compensationError) {
                e.addSuppressed(compensationError);
                log.error("秒杀订单落库失败，且 Redis 库存补偿失败。activityId={}, userId={}",
                        activityId, userId, compensationError);
            }
            log.error("秒杀请求投递 RabbitMQ 失败。activityId={}, userId={}", activityId, userId, e);
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                    "秒杀请求暂时无法提交，请稍后重试");
        }
    }

    public SeckillOrderStatusView getOrderStatus(Long activityId, Long userId) {
        SeckillOrder order = orderPersistenceService.findOrder(activityId, userId);
        if (order != null) {
            return new SeckillOrderStatusView("CREATED", order);
        }
        return new SeckillOrderStatusView(
                stockService.isReserved(activityId, userId) ? "PROCESSING" : "NOT_FOUND", null);
    }

    private ActivityView toActivityView(SeckillActivity activity) {
        Product product = productMapper.selectById(activity.getProductId());
        ActivityView view = new ActivityView();
        view.setActivityId(activity.getId());
        view.setProductId(activity.getProductId());
        view.setProductName(product == null ? "商品已删除" : product.getName());
        view.setOriginalPrice(product == null ? null : product.getPrice());
        view.setSeckillPrice(activity.getSeckillPrice());
        view.setStock(activity.getStock());
        view.setStartTime(activity.getStartTime());
        view.setEndTime(activity.getEndTime());
        return view;
    }
}
