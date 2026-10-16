package com.gyx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gyx.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper 接口
 * <p>
 * 继承 {@link BaseMapper}{@code <User>} 自动获得通用 CRUD：
 * insert / deleteById / updateById / selectById / selectList / selectOne / selectPage
 * <p>
 * 面试要点：MyBatis-Plus 通过 JDK 动态代理为 Mapper 接口生成实现，
 * 无需写 XML 或 SQL 即可完成单表操作。复杂查询可用 {@code LambdaQueryWrapper} 构建条件。
 *
 * @author gyx
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
