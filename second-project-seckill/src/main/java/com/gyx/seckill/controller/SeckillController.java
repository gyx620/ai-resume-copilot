package com.gyx.seckill.controller;

import com.gyx.seckill.common.ApiResponse;
import com.gyx.seckill.config.JwtAuthInterceptor;
import com.gyx.seckill.dto.ActivityView;
import com.gyx.seckill.dto.SeckillOrderStatusView;
import com.gyx.seckill.dto.SeckillRequestAccepted;
import com.gyx.seckill.service.SeckillService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/api/seckill")
public class SeckillController {

    private final SeckillService seckillService;

    public SeckillController(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    @GetMapping("/activities")
    public ApiResponse<List<ActivityView>> listActivities() {
        return ApiResponse.success(seckillService.listActivities());
    }

    @PostMapping("/activities/{activityId}/orders")
    public ResponseEntity<ApiResponse<SeckillRequestAccepted>> createOrder(
            @PathVariable Long activityId,
            @RequestAttribute(JwtAuthInterceptor.USER_ID_ATTRIBUTE) Long userId) {
        SeckillRequestAccepted accepted = seckillService.createOrder(activityId, userId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new ApiResponse<>(HttpStatus.ACCEPTED.value(), "秒杀请求已受理，订单处理中", accepted));
    }

    @GetMapping("/activities/{activityId}/orders/status")
    public ApiResponse<SeckillOrderStatusView> getOrderStatus(
            @PathVariable Long activityId,
            @RequestAttribute(JwtAuthInterceptor.USER_ID_ATTRIBUTE) Long userId) {
        return ApiResponse.success(seckillService.getOrderStatus(activityId, userId));
    }
}
