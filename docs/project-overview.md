# AI Resume Copilot — 面试讲解指南

---

## 一句话介绍

> 我做的是一个 **AI 简历分析助手**，前后端分离架构。后端 Spring Boot 3，前端 Vue 3 + Vite。用户上传 PDF 简历，后端用 PDFBox 解析文本，通过 LangChain4j 调用 DeepSeek 大模型，输出评分/优势/问题/优化建议。分析结果支持 **SSE 流式实时展示**（类似 ChatGPT 逐字输出）。还搭建了 **RAG 检索增强模块**，可以把知识库相关内容拼入 prompt 提升分析质量。

---

## 核心业务流程

```
注册/登录(JWT) → 上传PDF(PDFBox解析) → 简历列表(CRUD)
→ 选中简历 → 点击AI分析 → 拼装Prompt → 调用DeepSeek
→ SSE流式输出(前端逐字展示) → 解析JSON结果 → 存入MySQL + Redis缓存
→ 返回评分/优势/问题/建议
```

如果流式失败：**自动降级到非流式请求**，保证分析不中断。

---

## 技术架构

### 后端

| 技术 | 版本 | 为什么选它 |
|------|------|-----------|
| Spring Boot | 3.4.5 | 生态最全，自动配置省代码 |
| MyBatis-Plus | 3.5.7 | LambdaQueryWrapper 类型安全，BaseMapper 自带 CRUD |
| LangChain4j | 0.36.2 | 声明式 AI 调用，接口+注解即可，和 MyBatis 动态代理思路一致 |
| DeepSeek | deepseek-chat | OpenAI 兼容 API，成本低，国内访问快 |
| PDFBox | 3.0.3 | Apache 出品，纯 Java 无系统依赖 |
| JWT (jjwt) | 0.11.2 | 无状态认证，HMAC-SHA256 签名 |
| Redis | — | AI 分析结果缓存 30 分钟，减少重复调用 |
| BCrypt | spring-security-crypto | 密码单向加密，只引了 crypto 模块没引整个 Security 框架 |

### 前端

| 技术 | 为什么选它 |
|------|-----------|
| Vue 3 (Composition API) | `<script setup>` 语法简洁，ref/reactive 响应式 |
| Vite | 秒级热更新，比 Webpack 快一个数量级 |
| Element Plus | 企业级 UI 组件库，el-table/el-dialog/el-upload 开箱即用 |
| Vue Router 4 | SPA 路由，beforeEach 鉴权守卫 |
| Axios | 请求/响应拦截器统一处理 JWT 和业务错误 |

### 可替换性设计

`ChatLanguageModel` 是 LangChain4j 的抽象接口，`AiConfig` 里同时注入了 `ChatLanguageModel`（普通）和 `StreamingChatLanguageModel`（流式）两个 Bean。换 OpenAI / Claude / 文心一言**只需要改 baseUrl 和 modelName，不改业务代码**。

---

## 项目分层（后端）

```
controller/           ← 接口层：接收请求、参数校验、返回结果
  ├── UserController       POST /api/user/register, /login
  ├── ResumeUploadController  POST /api/resume/upload
  ├── ResumeRecordController  CRUD /api/resume/**
  └── AIController         POST /api/ai/analyze, /analyze/stream

service/              ← 业务层：接口 + 实现分离
  ├── UserService / impl      注册校验、登录签 JWT
  ├── ResumeUploadService     上传 + PDFBox 解析
  ├── ResumeRecordService     CRUD + 用户归属校验
  ├── AiService / impl        Prompt 拼装 → 调 AI → 缓存 → 入库
  └── FileService            通用文件上传

mapper/               ← 数据层：继承 BaseMapper<T>，零 SQL
entity/               ← 实体：@TableName + @TableId + @TableField
dto/  vo/            ← 入参 DTO + 出参 VO，和 Entity 分离
ai/                   ← AI 层
  ├── ResumeAssistant     LangChain4j 声明式接口(@SystemMessage + @UserMessage)
  └── PromptBuilder       动态构造 Prompt 模板
rag/                  ← RAG 检索增强
  ├── DocumentLoader      加载文档
  ├── TextSplitter        按句子切分 + 滑动窗口重叠
  ├── EmbeddingService    调 DeepSeek Embedding API 向量化 + 余弦相似度检索
  └── RagService          串联 pipeline，拼入增强 prompt
config/               ← 配置层
  ├── AiConfig            双模型 Bean(Chat + Streaming)
  ├── RedisConfig         StringRedisTemplate + Object RedisTemplate
  ├── WebConfig           JWT 拦截器注册
  └── WebMvcConfig        静态资源映射
interceptor/          ← JwtInterceptor: preHandle 解析 JWT → 存 ThreadLocal
exception/            ← GlobalExceptionHandler 三层异常兜底
utils/                ← JwtUtil / PasswordUtil / PdfUtil / UserContext / FileUtil
```

分层核心原则：
- **Controller**：只做参数接收+返回，加 `@Valid` 校验，不写业务逻辑
- **Service**：接口+实现分离，方便单测 mock；每个操作校验用户归属（`exist.getUserId().equals(currentUserId)`）
- **Mapper**：继承 `BaseMapper<T>`，自带 insert/deleteById/selectById/selectList
- **Interceptor + ThreadLocal**：JWT 解析后 userId 存 ThreadLocal，Service 层 `UserContext.getUserId()` 直接取，**避免层层传参**；`afterCompletion` 必须 `remove()` 防内存泄漏

---

## 前端架构

```
resume-frontend/
├── vite.config.js          Vite 配置 + /api 代理到 localhost:8080
├── src/
│   ├── main.js             注册 ElementPlus + Router + 全局图标
│   ├── App.vue             根组件，<router-view />
│   ├── router/index.js     路由表 + beforeEach 鉴权守卫
│   ├── api/index.js        Axios 实例 + 请求/响应拦截器 + 所有 API 封装
│   ├── stores/auth.js      登录状态管理(reactive 单例)
│   ├── views/
│   │   ├── Login.vue        登录页
│   │   ├── Register.vue     注册页
│   │   └── Dashboard.vue    工作台(上传/列表/分析/记录)
│   └── styles/global.css   全局样式
```

### 前端关键设计点

**1. Axios 响应拦截器 — 统一业务错误处理**
```javascript
http.interceptors.response.use(
  res => {
    const body = res.data
    // 即使 HTTP 200，body.code !== 200 也算失败
    if (body && typeof body.code === 'number' && body.code !== 200) {
      ElMessage.error(body.message)
      return Promise.reject(new Error(body.message))
    }
    return res
  },
  err => {
    // HTTP 401 → 清登录态 → 跳登录页
    if (err.response?.status === 401) {
      localStorage.clear()
      window.location.href = '/login'
    }
    ...
  }
)
```

**2. 路由鉴权守卫**
```javascript
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) next('/login')
  else next()
})
```

**3. 删除的乐观更新策略**
点击删除 → 立即从本地数组 splice 移除（界面瞬间消失）→ 发 DELETE 请求 → 成功则结束，失败则 push 回去并弹错误。不等后端响应直接更新 UI。

**4. AI 流式分析 + 降级**
优先用 SSE（`/api/ai/analyze/stream`），逐 token 展示。如果 SSE 失败，自动降级到普通请求（`/api/ai/analyze`），保证分析不中断。

---

## 踩坑记录 & 面试亮点

### 坑 1：MyBatis-Plus 主键策略导致"删不掉"

**现象**：上传简历后在列表里能看到，点击删除报"简历不存在"。
**根因**：实体没加 `@TableId`，MyBatis-Plus 默认用 `IdType.ASSIGN_ID`（雪花算法），生成 `838294473920454658` 这样的 19 位 ID。数据库列如果是 `INT(11)`，最大值 `2147483647`，雪花 ID 被截断。Java 对象里的 ID ≠ 数据库实际存储的 ID，`selectById` 自然找不到。
**解决**：加 `@TableId(type = IdType.AUTO)`，让 MySQL 自增 ID，Java 和 DB 的 ID 一致。

### 坑 2：简历上传路径冲突

`ResumeUploadController` 和 `ResumeRecordController` 都映射了 `/api/resume`，Spring Boot 启动时报路由冲突。解决：`ResumeUploadController` 去掉类级别 `@RequestMapping`，方法级别写完整路径 `/api/resume/upload`。

### 坑 3：静态文件被 JWT 拦截器拦截

`addPathPatterns("/**")` 导致 `/css/*` `/js/*` 也需要 JWT，未登录时前端白屏。改为 `addPathPatterns("/api/**")`，只拦截 API 请求。

### 坑 4：LangChain4j TokenStream 泛型不匹配

`TokenStream.onComplete()` 的参数类型是 `Consumer<Response<AiMessage>>` 而非 `Runnable`，直接传 `Runnable` 编译报错。需要适配：`.onComplete(response -> onComplete.run())`。

### 坑 5：DeepSeek baseUrl 缺少 `/v1`

DeepSeek 的 OpenAI 兼容端点是 `https://api.deepseek.com/v1/chat/completions`。LangChain4j 的 `OpenAiChatModel` 默认 baseUrl 是 `https://api.openai.com/v1`。如果设 `baseUrl("https://api.deepseek.com")`（缺少 `/v1`），实际请求变成 `https://api.deepseek.com/chat/completions`，可能不兼容。正确写法：`baseUrl("https://api.deepseek.com/v1")`。

---

## 面试高频问题 & 回答要点

### Q1：你这个项目的难点在哪？

1. **AI 调用不是简单 HTTP 请求** — 用了 LangChain4j 的声明式代理（`@SystemMessage` + `@UserMessage`），和 MyBatis 动态代理一个思路，框架自动拼 prompt 调 LLM。而且同时支持普通和流式两种模式（`ChatLanguageModel` + `StreamingChatLanguageModel` 双 Bean），流式失败自动降级。

2. **RAG 检索增强** — 不是把简历直接丢给 AI。先用 TextSplitter 按句子切分（滑动窗口 50 字重叠防截断），EmbeddingService 调 DeepSeek Embedding API 向量化，余弦相似度计算 topK 相关片段，拼入 prompt。这个 pipeline 是我自己实现的，没有用现成的向量数据库。架构上接口是抽象的，后续换 Milvus/Pgvector 只需改实现类。

3. **SSE 流式返回** — 普通接口要等 AI 全部生成完才返回。我用 `StreamingChatLanguageModel` + `TokenStream` + `SseEmitter` 实现了 token 级别流式输出，前端逐字展示。而且流式失败会自动降级到 `/api/ai/analyze`（非流式），不会中断用户体验。

4. **前后端分离** — 前端 Vue 3 + Vite + Element Plus，Axios 拦截器统一处理 JWT 和业务错误码，路由守卫做鉴权。删除操作用乐观更新（先 splice 再发请求），用户体验好。

### Q2：JWT 认证流程是怎样的？为什么不用 Session？

- 登录成功 → jjwt 签 Token（HMAC-SHA256，payload 存 userId，24h 过期）→ 返回给前端
- 前端存 localStorage → 后续请求 Axios 拦截器自动带 `Authorization: Bearer <token>`
- 后端 `JwtInterceptor.preHandle()` 验签 → 提取 userId 存 ThreadLocal → 业务代码 `UserContext.getUserId()` 直接取
- `afterCompletion` 必须 `ThreadLocal.remove()`，防内存泄漏

不用 Session 因为 JWT 无状态，水平扩展不需要 session 同步。

### Q3：MyBatis-Plus 的 LambdaQueryWrapper 优势？

```java
// 字符串字段名（字段改名就炸）
.eq("username", username)
// Lambda 方式（编译器检查，重构自动同步）
.eq(User::getUsername, username)
```
类型安全，IDE 自动补全，字段改名后编译报错。

### Q4：Prompt 怎么设计的？

两层：
1. `@SystemMessage` — 设定 AI 角色：Java 后端面试官
2. `PromptBuilder` — 动态构造用户消息，填入简历内容，要求返回结构化 JSON（score/advantage/problem/suggestion）

为什么提到 PromptBuilder 里：方便后续 A/B 测试和 prompt 调优，改 prompt 不用改 Java 代码。

### Q5：Redis 怎么用的？

- 缓存 AI 分析结果，key = `resume:ai:{resumeId}`，过期 30 分钟
- 用 `StringRedisTemplate` 存 JSON 字符串，读出来 `ObjectMapper` 反序列化
- 缓存命中直接返回，不调 AI，省钱也快
- 缓存解析失败会删掉旧缓存，走真实 AI 调用

### Q6：异常处理怎么做的？

三层兜底：
1. `BusinessException(code, message)` → `GlobalExceptionHandler` 返回对应 code
2. `MethodArgumentNotValidException`（`@Valid` 校验失败）→ 统一拼接 `"字段名: 错误信息"`
3. `Exception` 兜底 → 返回 500

`@RestControllerAdvice` AOP 统一拦截，业务代码不用到处 try-catch。

### Q7：`@TableId` 的 `IdType.AUTO` vs `ASSIGN_ID` 有什么区别？

- `AUTO`：数据库自增主键，MyBatis-Plus 不设 ID，INSERT 后读 `getGeneratedKeys()` 回填
- `ASSIGN_ID`（默认）：雪花算法生成 19 位 Long ID，MyBatis-Plus 在 INSERT 前设置

如果数据库列是 `INT`，雪花 ID 会截断，导致 Java 对象的 ID ≠ DB 实际 ID。这就是我踩的坑。

### Q8：ThreadLocal 为什么要 remove？

ThreadLocal key 是弱引用会被 GC，但 value 是强引用。如果线程被线程池复用，上一次请求的 value 会残留在下一次请求中，导致：
- 读到一个"幽灵用户"的 userId
- 内存泄漏（value 永远不会被回收）

所以 `afterCompletion` 必须 `remove()`。

### Q9：前端 Axios 拦截器怎么处理业务错误的？

后端统一返回 `{code, message, data}`。即使 HTTP 状态码是 200，`code !== 200` 也算失败。响应拦截器检测到这个情况就 `ElMessage.error()` + `Promise.reject()`，调用方 catch 即可。

### Q10：如果再给你时间，会优化哪些地方？

1. 密钥/密码抽到环境变量或配置中心，不硬编码 yml
2. AI 分析异步化——提交任务返回 taskId，后台处理，轮询/WebSocket 通知结果
3. 上向量数据库（Milvus/Pgvector）替代内存检索
4. Docker Compose 一键部署（MySQL + Redis + 后端 + 前端 Nginx）
5. 前端加 TypeScript、Pinia 状态管理、单元测试（Vitest）

---

## 项目结构速查

| 文件 | 亮点 |
|------|------|
| `config/AiConfig.java` | ChatLanguageModel + StreamingChatLanguageModel 双 Bean，可替换式架构 |
| `ai/ResumeAssistant.java` | `@SystemMessage` + `@UserMessage` 声明式 AI 调用 |
| `ai/PromptBuilder.java` | Prompt 模板化，支持 A/B 测试 |
| `interceptor/JwtInterceptor.java` | ThreadLocal 存取 + remove 防泄漏 |
| `service/impl/AiServiceImpl.java` | Redis 缓存 + 流式调用 + JSON 清洗 + SSE 降级 |
| `service/impl/ResumeRecordServiceImpl.java` | CRUD + 用户归属校验 + @Slf4j 日志 |
| `controller/AIController.java` | 普通分析 + SSE 流式分析 双端点 |
| `rag/TextSplitter.java` | 按句子切分 + 滑动窗口重叠 |
| `rag/EmbeddingService.java` | DeepSeek Embedding 向量化 + 余弦相似度 topK |
| `rag/RagService.java` | 完整 RAG pipeline：加载→切分→向量化→检索→拼 prompt |
| `exception/GlobalExceptionHandler.java` | BusinessException + Validation + Exception 三层兜底 |
| `entity/ResumeRecord.java` | @TableId(AUTO) + @TableField 显式映射 |
| **前端** | |
| `src/router/index.js` | beforeEach 鉴权守卫 |
| `src/api/index.js` | Axios 拦截器 + 业务错误统一处理 |
| `src/views/Dashboard.vue` | 乐观删除 + SSE 流式展示 + 降级处理 |
| `src/stores/auth.js` | reactive 单例管理登录态 |
| `vite.config.js` | `/api` 代理到后端，免 CORS 配置 |
