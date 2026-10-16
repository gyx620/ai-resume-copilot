package com.gyx.config;


import com.gyx.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置类 —— 注册拦截器
 * <p>
 * {@code @Configuration} 标识本类为 Spring 配置类，启动时加载。
 * 实现 {@link WebMvcConfigurer} 以自定义 MVC 行为（此处为添加拦截器）。
 * <p>
 * 拦截规则：所有请求默认需要携带 JWT Token，
 * 仅 {@code /api/user/login} 和 {@code /api/user/register} 放行。
 *
 * @author gyx
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** JWT 认证拦截器，校验请求头中的 Authorization */
    @Autowired
    private JwtInterceptor jwtInterceptor;

    /**
     * 注册拦截器到 Spring MVC 拦截器链
     * <p>
     * {@code addPathPatterns("/**")}  —— 拦截所有请求
     * {@code excludePathPatterns(...)} —— 排除登录和注册接口（无需携带 Token）
     * <p>
     * 面试要点：拦截器 vs 过滤器
     * <ul>
     *   <li>拦截器（Interceptor）属于 Spring MVC，能拿到 Handler 上下文</li>
     *   <li>过滤器（Filter）属于 Servlet 容器，在 DispatcherServlet 之前执行</li>
     * </ul>
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/user/login",
                        "/api/user/register"
                );
    }
}
