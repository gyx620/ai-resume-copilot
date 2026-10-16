package com.gyx.seckill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 32, message = "用户名长度为 3 到 32 个字符")
        @Pattern(regexp = "[a-zA-Z0-9_]+", message = "用户名只能包含字母、数字和下划线")
        String username,
        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 64, message = "密码长度为 8 到 64 个字符")
        String password) {
}
