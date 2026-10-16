package com.gyx.seckill.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gyx.seckill.dto.LoginRequest;
import com.gyx.seckill.dto.LoginResponse;
import com.gyx.seckill.dto.RegisterRequest;
import com.gyx.seckill.entity.AppUser;
import com.gyx.seckill.exception.BusinessException;
import com.gyx.seckill.mapper.AppUserMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AppUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(AppUserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    public void register(RegisterRequest request) {
        AppUser user = new AppUser();
        user.setUsername(request.username().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(HttpStatus.CONFLICT, "用户名已被注册");
        }
    }

    public LoginResponse login(LoginRequest request) {
        AppUser user = userMapper.selectOne(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getUsername, request.username().trim()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        return new LoginResponse(jwtTokenService.createToken(user.getId()), "Bearer",
                jwtTokenService.getExpirationMs());
    }
}
