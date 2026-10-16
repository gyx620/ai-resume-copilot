# 高并发秒杀商城

一个用于逐步学习并发库存控制的 Java 项目。当前版本使用 Redis Lua 原子预扣库存、RabbitMQ 异步创建订单、MySQL 事务落单，并为消息投递失败提供 Redis 库存补偿。

## 技术栈

- Java 17、Spring Boot 3.4.5
- MyBatis-Plus、MySQL
- 当前阶段：Redis、Lua、RabbitMQ、MySQL 事务
- 当前功能：JWT 登录认证

## 当前阶段目标

1. 查询商品与秒杀活动。
2. 用户提交秒杀请求并创建订单。
3. 同一个用户在同一个活动中只能成功一次。
4. 并发请求下库存不能变成负数。

Redis Lua 脚本把“判断是否重复购买、检查库存、扣减库存、记录用户”作为一个原子操作，避免多个请求同时读到相同库存。预扣成功后，应用把订单消息发送到 RabbitMQ 并等待 Broker 确认，接口返回 HTTP 202。消费者再使用 MySQL 事务扣数据库库存并写入订单；业务规则拒绝时释放 Redis 预扣，未处理的异常会重试并在失败后进入死信队列。MySQL 的条件更新和唯一索引仍是最后一道保护。

## 数据模型

- `product`：商品基本信息与日常价格。
- `seckill_activity`：秒杀价格、活动时间和活动库存。
- `seckill_order`：用户购买记录；`(user_id, activity_id)` 唯一约束作为防重复购买的最后一道保护。

## 本地准备

1. 使用 MySQL 8，运行 `docs/init.sql` 创建数据库、表和演示活动。
2. 在项目根目录的 `application-local.yml` 中填写本机 MySQL 密码。该文件已被 Git 忽略。
3. 使用 Java 17 运行 `mvn spring-boot:run`。

后端默认运行在 `http://localhost:8081`，避免与简历助手项目的 8080 端口冲突。

## 当前接口

- `GET /api/seckill/activities`：查看秒杀活动。
- `POST /api/auth/register`：注册账号。
- `POST /api/auth/login`：登录并取得 JWT。
- `POST /api/seckill/activities/{activityId}/orders`：提交秒杀请求，返回 `202 Accepted`，表示订单正在处理中。
- `GET /api/seckill/activities/{activityId}/orders/status`：查询当前登录用户的订单状态。`PROCESSING` 表示还在处理中，`CREATED` 表示已落库，`NOT_FOUND` 表示当前没有预扣请求。

注册和登录示例：

```json
{
  "username": "demo_user",
  "password": "demo-pass-123"
}
```

登录后，把响应里的 `data.token` 放进请求头 `Authorization: Bearer <token>`。秒杀接口和订单状态接口都从 JWT 中取得用户 ID，不接受客户端自行填写的 `userId`。密码用 BCrypt 哈希后保存，不以明文写入数据库。

PowerShell 端到端试用示例（把活动 ID 替换成活动列表中实际返回的 ID）：

```powershell
$registerBody = '{"username":"demo_user","password":"demo-pass-123"}'
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8081/api/auth/register" `
  -ContentType "application/json" `
  -Body $registerBody

$loginResult = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8081/api/auth/login" `
  -ContentType "application/json" `
  -Body $registerBody

$authHeaders = @{ Authorization = "Bearer $($loginResult.data.token)" }

Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8081/api/seckill/activities/1/orders" `
  -Headers $authHeaders

Invoke-RestMethod -Method Get `
  -Uri "http://localhost:8081/api/seckill/activities/1/orders/status" `
  -Headers $authHeaders
```

## 当前阶段的并发保护

应用启动时，会用数据库库存初始化 Redis 中尚不存在的库存键，不会覆盖已有 Redis 库存。秒杀请求由 Lua 在 Redis 中原子预扣，消息发布者等待 RabbitMQ 发布确认后返回请求受理。消费者调用 `SeckillOrderPersistenceService`，由独立 Spring 事务更新 MySQL 并创建订单。消息失败会最多重试 3 次，之后进入死信队列 `seckill.order.dead.queue`，便于排查。

## 面试讲解与验证记录

项目介绍、下单链路、关键设计、常见问题和本机并发验证记录见 [docs/INTERVIEW_GUIDE.md](docs/INTERVIEW_GUIDE.md)。目前完成的是 10 个用户的本机并发功能验证，不代表生产环境性能或最大承载能力。

服务层单元测试位于 `src/test/java`，覆盖正常受理、重复用户和库存不足分支；测试使用 Mockito 隔离数据库、Redis 和 RabbitMQ，不需要启动这些服务。运行方式：

```powershell
mvn test
```

这组单元测试验证秒杀业务分支，不替代真实 Redis、RabbitMQ、MySQL 环境下的集成测试或性能测试。

JWT 使用 HS256 签名，默认有效期为 24 小时。开发密钥只用于本地运行；部署时设置至少 32 字节的 `JWT_SECRET` 环境变量，并重启服务。

Redis 键：

- `seckill:stock:{activityId}`：活动剩余库存。
- `seckill:buyers:{activityId}`：已成功预扣库存的用户集合。

本地需要同时运行 MySQL、Redis 和 RabbitMQ。已安装 Docker Desktop 时，可分别在 PowerShell 中运行：

```powershell
docker run --name seckill-redis -p 6379:6379 -d redis:7
docker run --name seckill-rabbitmq -p 5672:5672 -p 15672:15672 -d rabbitmq:3-management
```

RabbitMQ 管理页面是 `http://localhost:15672`，本地默认账号密码为 `guest` / `guest`。死信队列中的消息表示订单消费者多次失败，排查后再处理，不要直接清空；对应 Redis 预扣库存仍被保留，避免失败消息被当成可用库存。

启动项目后端：

```powershell
cd D:\com.gyx\ai-resume-copilot\second-project-seckill
mvn spring-boot:run
```

注意：Redis、RabbitMQ 和 MySQL 之间不是一个分布式事务。发布确认、消费者重试、死信队列和数据库唯一索引覆盖了常见失败场景；进程在 Redis 预扣与消息投递之间突然断电等极端情况，仍需后续用事务消息、Outbox 或库存对账机制完善。演示环境重置库存时，需要同步处理数据库和 Redis 对应活动的库存与用户集合。
