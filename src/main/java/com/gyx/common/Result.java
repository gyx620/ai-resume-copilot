package com.gyx.common;

import lombok.Data;

/**
 * 统一 API 响应体（泛型）
 * <p>
 * 所有 Controller 返回此类型，前端收到统一 JSON 结构：
 * <pre>{@code
 * {
 *   "code": 200,
 *   "message": "success",
 *   "data": ...
 * }
 * }</pre>
 * <p>
 * 面试要点：泛型 {@code <T>} 让 data 字段可以承载任意类型；
 * 静态工厂方法（success/error）比直接 new 构造器更语义化，是常见的设计模式。
 *
 * @param <T> data 字段的类型
 * @author gyx
 */
@Data
public class Result<T> {

    /** 业务状态码：200=成功，500=服务端异常 */
    private Integer code;

    /** 提示消息 */
    private String message;

    /** 响应数据，泛型支持任意类型 */
    private T data;

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功响应：code=200, message="success"
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    /**
     * 失败响应：code=500, data=null, message=异常描述
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

}
