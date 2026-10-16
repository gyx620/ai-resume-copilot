package com.gyx.seckill.dto;

public record LoginResponse(String token, String tokenType, long expiresIn) {
}
