package com.gyx.seckill.service;

import com.gyx.seckill.dto.SeckillRequestAccepted;
import com.gyx.seckill.entity.Product;
import com.gyx.seckill.entity.SeckillActivity;
import com.gyx.seckill.exception.BusinessException;
import com.gyx.seckill.mapper.ProductMapper;
import com.gyx.seckill.mapper.SeckillActivityMapper;
import com.gyx.seckill.messaging.SeckillOrderMessage;
import com.gyx.seckill.messaging.SeckillOrderPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeckillServiceTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private SeckillActivityMapper activityMapper;

    @Mock
    private RedisSeckillStockService stockService;

    @Mock
    private SeckillOrderPublisher orderPublisher;

    @Mock
    private SeckillOrderPersistenceService orderPersistenceService;

    @InjectMocks
    private SeckillService seckillService;

    private SeckillActivity activity;
    private Product product;

    @BeforeEach
    void setUp() {
        activity = new SeckillActivity();
        activity.setId(1L);
        activity.setProductId(2L);
        activity.setSeckillPrice(new BigDecimal("99.00"));
        activity.setStatus(1);
        activity.setStartTime(LocalDateTime.now().minusMinutes(1));
        activity.setEndTime(LocalDateTime.now().plusMinutes(10));

        product = new Product();
        product.setId(2L);
        product.setStatus(1);
    }

    @Test
    void createOrderPublishesMessageWhenReservationSucceeds() {
        when(activityMapper.selectById(1L)).thenReturn(activity);
        when(productMapper.selectById(2L)).thenReturn(product);
        when(stockService.reserve(1L, 7L)).thenReturn(1L);

        SeckillRequestAccepted accepted = seckillService.createOrder(1L, 7L);

        assertEquals(1L, accepted.activityId());
        assertEquals(7L, accepted.userId());
        assertEquals("PROCESSING", accepted.status());

        ArgumentCaptor<SeckillOrderMessage> messageCaptor =
                ArgumentCaptor.forClass(SeckillOrderMessage.class);
        verify(orderPublisher).publish(messageCaptor.capture());
        assertEquals(1L, messageCaptor.getValue().activityId());
        assertEquals(7L, messageCaptor.getValue().userId());
        assertEquals(2L, messageCaptor.getValue().productId());
        assertEquals(new BigDecimal("99.00"), messageCaptor.getValue().seckillPrice());
    }

    @Test
    void createOrderRejectsDuplicateUserWithoutPublishingMessage() {
        when(activityMapper.selectById(1L)).thenReturn(activity);
        when(productMapper.selectById(2L)).thenReturn(product);
        when(stockService.reserve(1L, 7L)).thenReturn(-1L);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> seckillService.createOrder(1L, 7L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(orderPublisher, never()).publish(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createOrderRejectsSoldOutActivityWithoutPublishingMessage() {
        when(activityMapper.selectById(1L)).thenReturn(activity);
        when(productMapper.selectById(2L)).thenReturn(product);
        when(stockService.reserve(1L, 7L)).thenReturn(0L);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> seckillService.createOrder(1L, 7L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(orderPublisher, never()).publish(org.mockito.ArgumentMatchers.any());
    }
}
