package com.gyx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gyx.dto.LoginDTO;
import com.gyx.dto.RegisterDTO;
import com.gyx.entity.User;
import com.gyx.mapper.UserMapper;
import com.gyx.service.UserService;
import com.gyx.utils.JwtUtil;
import com.gyx.utils.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 用户业务实现
 * <p>
 * 面试要点：{@code LambdaQueryWrapper} 使用方法引用（{@code User::getUsername}）
 * 代替字符串字段名，类型安全、编译器可检查、重构时自动同步。
 *
 * @author gyx
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 注册：校验用户名唯一 → BCrypt 加密 → 入库
     */
    @Override
    public void register(RegisterDTO registerDTO) {
        // 1. 查重：LambdaQueryWrapper 构建等值查询
        User existUser = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, registerDTO.getUsername()));
        if (existUser != null) {
            throw new RuntimeException("用户已存在");
        }

        // 2. 构建实体，密码 BCrypt 加密
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(PasswordUtil.encode(registerDTO.getPassword()));
        user.setEmail(registerDTO.getEmail());

        // 3. 调用 BaseMapper.insert 入库
        userMapper.insert(user);
    }

    /**
     * 登录：查用户 → BCrypt 比对密码 → 签发 JWT
     */
    @Override
    public String login(LoginDTO loginDTO) {
            User user = userMapper.selectOne(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getUsername, loginDTO.getUsername())
            );

            if (user == null) {
                throw new RuntimeException("用户不存在");
            }

            // 密码校验
            boolean result = PasswordUtil.matches(
                    loginDTO.getPassword(),
                    user.getPassword()
            );

            if (!result) {
                throw new RuntimeException("密码错误");
            }

            // 生成 JWT
            return jwtUtil.createToken(user.getId());
        }
}
