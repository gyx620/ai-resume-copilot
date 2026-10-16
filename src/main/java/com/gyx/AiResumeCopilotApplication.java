package com.gyx;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI 简历助手 - 启动类
 * <p>
 * {@code @SpringBootApplication} 是组合注解，等价于：
 * {@code @SpringBootConfiguration} + {@code @EnableAutoConfiguration} + {@code @ComponentScan}
 * <p>
 * 面试要点：自动配置原理 —— Spring Boot 通过 {@code spring.factories} 文件加载
 * 各 starter 的自动配置类，根据 classpath 中存在的 jar 和用户配置决定是否启用。
 *
 * @author gyx
 */
@MapperScan("com.gyx.mapper")
@SpringBootApplication
public class AiResumeCopilotApplication {

    /**
     * 入口：{@link SpringApplication#run} 创建 IoC 容器 → 扫描 Bean → 启动内嵌 Tomcat
     */
    public static void main(String[] args) {
        SpringApplication.run(AiResumeCopilotApplication.class, args);
    }

}
