package com.gyx.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt 密码工具类
 * <p>
 * BCrypt 特点（面试常考）：
 * <ul>
 *   <li><b>盐值内置</b>：每次 encode 自动生成随机盐值，相同明文产生不同密文 → 抗彩虹表</li>
 *   <li><b>不可逆</b>：无法从密文反推明文，只能通过 matches 比对</li>
 *   <li><b>工作因子</b>：strength=10 表示 2^10=1024 轮哈希，可调大应对算力增长</li>
 * </ul>
 * <p>
 * 依赖仅 {@code spring-security-crypto}，无需完整 Spring Security。
 * {@link BCryptPasswordEncoder} 线程安全，单例即可。
 *
 * @author gyx
 */
public class PasswordUtil {

    /** 加密器单例，默认工作因子=10 */
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * BCrypt 加密（含随机盐值）
     * @param password 明文
     * @return 60 字符密文，格式 {@code $2a$10$<salt><hash>}
     */
    public static String encode(String password) {
        return encoder.encode(password);
    }

    /**
     * 密文比对（从密文中提取盐值，用相同盐值重算后比较）
     * @param rawPassword  明文
     * @param encodedPassword 密文
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }

}
