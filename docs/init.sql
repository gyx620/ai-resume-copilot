-- 创建数据库
CREATE DATABASE IF NOT EXISTS resume_ai DEFAULT CHARSET utf8mb4;
USE resume_ai;

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    avatar VARCHAR(255),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 简历记录表
CREATE TABLE IF NOT EXISTS resume_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    file_name VARCHAR(255),
    file_url VARCHAR(500),
    content LONGTEXT,
    ai_score INT,
    ai_result TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- AI 分析记录表
CREATE TABLE IF NOT EXISTS ai_analysis_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resume_id BIGINT,
    score INT,
    advantage TEXT,
    problem TEXT,
    suggestion TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- AI 分析结果汇总表
CREATE TABLE IF NOT EXISTS resume_analysis (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resume_id BIGINT,
    user_id BIGINT,
    result TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
