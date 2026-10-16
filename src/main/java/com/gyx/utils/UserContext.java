package com.gyx.utils;

/**
 * 用户上下文 —— 基于 ThreadLocal 的当前请求用户缓存
 * <p>
 * 面试要点：ThreadLocal 为每个线程提供独立的变量副本，线程隔离。
 * 在拦截器中解析 JWT 后将 userId 存入，Service 层通过 {@code getUserId()} 获取，
 * 避免层层传参。注意：请求结束后必须调用 {@code remove()} 防止内存泄漏。
 *
 * @author gyx
 */
public class UserContext {
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    /** 请求结束后清理，防止内存泄漏（ThreadLocal key 是弱引用，但 value 是强引用） */
    public static void remove() {
        USER_ID.remove();
    }
}
