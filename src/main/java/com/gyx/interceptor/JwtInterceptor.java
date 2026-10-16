package com.gyx.interceptor;

import com.gyx.utils.JwtUtil;
import com.gyx.utils.UserContext;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * JWT 认证拦截器
 * <p>
 * 实现 {@link HandlerInterceptor#preHandle}，在 Controller 方法执行前：
 * <ol>
 *   <li>从请求头 {@code Authorization} 中提取 Token</li>
 *   <li>Token 为空 → 返回 401，阻断请求</li>
 *   <li>Token 存在 → 解析 JWT 获取 userId → 存入 {@link UserContext}</li>
 * </ol>
 * <p>
 * 面试要点：{@code preHandle} 返回 true=放行，false=阻断；
 * 请求结束后通过 {@code afterCompletion} 清理 ThreadLocal，防止内存泄漏。
 *
 * @author gyx
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {

        String token = request.getHeader("Authorization");


        if (token == null || token.isEmpty()) {
            response.setStatus(401);
            return false;
        }


        // Authorization: Bearer xxx.xxx.xxx
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }


        try {
            Long userId = jwtUtil.getUserId(token);
            UserContext.setUserId(userId);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
    }


    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {

        UserContext.remove();

    }
}
