CREATE DATABASE IF NOT EXISTS seckill_mall
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE seckill_mall;

CREATE TABLE IF NOT EXISTS app_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(32) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS product (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    price DECIMAL(10, 2) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=上架, 0=下架',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT chk_product_price CHECK (price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS seckill_activity (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    seckill_price DECIMAL(10, 2) NOT NULL,
    stock INT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用, 0=停用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_activity_status_time (status, start_time, end_time),
    CONSTRAINT fk_activity_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT chk_activity_price CHECK (seckill_price >= 0),
    CONSTRAINT chk_activity_stock CHECK (stock >= 0),
    CONSTRAINT chk_activity_time CHECK (end_time > start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS seckill_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_no VARCHAR(32) NOT NULL,
    user_id BIGINT NOT NULL COMMENT '第一阶段用演示用户 ID；后续接入登录',
    activity_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(10, 2) NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0=已创建, 1=已取消',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    UNIQUE KEY uk_user_activity (user_id, activity_id),
    KEY idx_order_user_time (user_id, create_time),
    CONSTRAINT fk_order_activity FOREIGN KEY (activity_id) REFERENCES seckill_activity (id),
    CONSTRAINT fk_order_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT chk_order_quantity CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO product (name, description, price, status)
SELECT '演示机械键盘', '秒杀流程演示商品', 399.00, 1
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = '演示机械键盘');

INSERT INTO seckill_activity (product_id, seckill_price, stock, start_time, end_time, status)
SELECT id, 99.00, 20, NOW() - INTERVAL 1 MINUTE, NOW() + INTERVAL 30 DAY, 1
FROM product
WHERE name = '演示机械键盘'
  AND NOT EXISTS (SELECT 1 FROM seckill_activity WHERE product_id = product.id);

INSERT INTO seckill_activity
(product_id, seckill_price, stock, start_time, end_time, status)
SELECT
    product_id, seckill_price, 100,
    NOW() - INTERVAL 1 MINUTE,
    NOW() + INTERVAL 1 DAY,
    1
FROM seckill_activity
WHERE id = 1;

INSERT INTO seckill_activity
(product_id, seckill_price, stock, start_time, end_time, status)
SELECT
    product_id, seckill_price, 100,
    NOW() - INTERVAL 1 MINUTE,
    NOW() + INTERVAL 1 DAY,
    1
FROM seckill_activity
WHERE id = 1;

USE seckill_mall;
SHOW TABLES;

SELECT
    u.username,
    o.activity_id,
    o.status,
    o.create_time
FROM seckill_order o
         JOIN app_user u ON u.id = o.user_id
WHERE o.activity_id = 3
ORDER BY o.create_time;

SELECT u.username, o.activity_id, o.status, o.create_time
FROM seckill_order o
         JOIN app_user u ON u.id = o.user_id
WHERE o.activity_id = 3
  AND u.username BETWEEN 'loadtest11' AND 'loadtest20'
ORDER BY u.username;