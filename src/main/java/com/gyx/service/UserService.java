package com.gyx.service;

import com.gyx.dto.LoginDTO;
import com.gyx.dto.RegisterDTO;

/**
 * 用户业务接口
 *
 * @author gyx
 */
public interface UserService {

    /**
     * 注册：校验用户名唯一 → BCrypt 加密密码 → 写入数据库
     *
     * @throws RuntimeException 用户名已存在时抛出
     */
    void register(RegisterDTO registerDTO);

    /**
     * 登录：查用户 → BCrypt 比对密码 → 签发 JWT Token
     *
     * @return JWT Token 字符串，前端后续请求携带此 Token 进行身份认证
     * @throws RuntimeException 用户不存在或密码错误时抛出
     */
    String login(LoginDTO loginDTO);

}
