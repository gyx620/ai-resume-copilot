package com.gyx.seckill.service;

import com.gyx.seckill.entity.SeckillActivity;
import com.gyx.seckill.mapper.SeckillActivityMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class SeckillStockInitializer implements ApplicationRunner {

    private final SeckillActivityMapper activityMapper;
    private final RedisSeckillStockService stockService;

    public SeckillStockInitializer(SeckillActivityMapper activityMapper,
                                   RedisSeckillStockService stockService) {
        this.activityMapper = activityMapper;
        this.stockService = stockService;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (SeckillActivity activity : activityMapper.selectList(null)) {
            if (activity.getStock() != null && activity.getStock() >= 0) {
                stockService.initializeStock(activity.getId(), activity.getStock());
            }
        }
    }
}
