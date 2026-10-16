package com.gyx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gyx.dto.LoginDTO;
import com.gyx.dto.RegisterDTO;
import com.gyx.entity.User;
import com.gyx.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void register_shouldSucceed_whenUsernameNotExists() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("testuser");
        dto.setPassword("123456");
        dto.setEmail("test@example.com");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        doReturn(1).when(userMapper).insert(any(User.class));

        assertDoesNotThrow(() -> userService.register(dto));
        verify(userMapper).selectOne(any(LambdaQueryWrapper.class));
        verify(userMapper).insert(any(User.class));
    }

    @Test
    void register_shouldThrow_whenUsernameExists() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("existing");
        dto.setPassword("123456");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(new User());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.register(dto));
        assertEquals("用户已存在", ex.getMessage());
    }

    @Test
    void login_shouldThrow_whenUserNotFound() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("nonexistent");
        dto.setPassword("123456");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.login(dto));
        assertEquals("用户不存在", ex.getMessage());
    }
}
