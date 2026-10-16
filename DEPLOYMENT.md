# 两个项目的演示环境部署准备

仓库根目录的 `compose.yaml` 会启动 Vue 简历助手前端、两个 Spring Boot 后端、MySQL、Redis 和 RabbitMQ。公网只映射 HTTP 80 端口；数据库、Redis、RabbitMQ 管理端口不映射到公网。

## 准备服务器

- Linux x86_64 云服务器，安装 Docker Engine 和 Docker Compose plugin。
- 演示环境建议至少 4 vCPU / 8 GB RAM；这是两个 Java 应用与 MySQL、Redis、RabbitMQ 同机运行的起步估算，不是性能保证。
- 若使用中国大陆服务器并绑定域名对公网提供网站，先完成 ICP 备案。

## 配置和启动

1. 在服务器克隆项目。
2. 将 `.env.example` 复制为 `.env`，为每个变量生成独立强随机值；填入新签发的 DeepSeek API Key。不要把 `.env` 提交到代码仓库或发到聊天里。
3. 执行 `docker compose up -d --build`。
4. 查看 `docker compose ps` 和 `docker compose logs -f resume-api seckill-api`。
5. 用 `http://服务器公网IP/` 打开简历助手；秒杀 API 可通过 `/seckill-api/` 前缀访问。

## 注意

- MySQL 初始化脚本只在 `mysql_data` 空卷首次创建时执行。不要为了重跑初始化而随意删除数据卷。
- 当前配置适合学习/作品集演示，不是生产级安全方案；正式服务应使用专门的数据库账户、备份、HTTPS、监控和更严格的资源隔离。
- `.env` 中 DeepSeek Key 会产生第三方 API 调用费用，应设置账户限额并避免公开分享站点凭据。
- 国内云厂商的备案要求与流程以所选厂商官方说明为准。
