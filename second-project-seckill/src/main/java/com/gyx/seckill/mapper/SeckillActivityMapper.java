package com.gyx.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gyx.seckill.entity.SeckillActivity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface SeckillActivityMapper extends BaseMapper<SeckillActivity> {

    @Update("UPDATE seckill_activity "
            + "SET stock = stock - 1 "
            + "WHERE id = #{id} AND stock > 0")
    int decreaseStockIfAvailable(@Param("id") Long id);
}
