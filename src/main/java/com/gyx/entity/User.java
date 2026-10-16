package com.gyx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体，映射 {@code sys_user} 表
 * <p>
 * {@code @TableName} 显式指定表名（类名与表名不一致时使用）。
 * 密码字段存储 BCrypt 密文，不可逆。
 *
 * @author gyx
 */
@Data
@TableName("sys_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名，唯一 */
    private String username;

    /** BCrypt 加密后的密码密文，不可逆 */
    private String password;

    /** 邮箱 */
    private String email;

    /** 头像 URL */
    private String avatar;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

}
