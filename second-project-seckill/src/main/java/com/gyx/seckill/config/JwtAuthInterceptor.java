package com.gyx.seckill.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gyx.seckill.common.ApiResponse;
import com.gyx.seckill.service.JwtTokenService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    public static final String USER_ID_ATTRIBUTE = "loginUserId";

    private final JwtTokenService tokenService;
    private final ObjectMapper objectMapper;

    public JwtAuthInterceptor(JwtTokenService tokenService, ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            writeUnauthorized(response);
            return false;
        }

        try {
            Long userId = tokenService.parseUserId(authorization.substring(7).trim());
            request.setAttribute(USER_ID_ATTRIBUTE, userId);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            writeUnauthorized(response);
            return false;
        }
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                new ApiResponse<>(HttpServletResponse.SC_UNAUTHORIZED, "登录凭证无效或已过期", null));
    }
}
