---
name: gomoku-spec
description: 五子棋在线对战项目的可验收规格与测试映射。用于 AI 编码、评审与回归时约束功能、安全、协议与性能边界。
---

# 五子棋在线对战 · 可验收规格（Skill）

## 1. 目标

在 Java 17 + Spring Boot 3 + MySQL 8 + Spring Data JPA 的约束下，交付一个可运行、可测试、可部署的在线五子棋系统：
注册登录、按评分匹配、15×15 对局、对局聊天、ELO 天梯、断线重连。服务端是唯一权威，客户端只渲染。

## 2. 范围

**做**：注册/登录/退出、匹配、对局、聊天、排行榜、断线重连、计时、认输、安全加固、部署文件。
**不做**：真 P2P/WebRTC、观战、好友房、悔棋、禁手、头像上传、多实例集群。

## 3. 角色

- 玩家：注册、登录、匹配、对局、聊天、查看排名。
- 服务端：认证、匹配、裁决、计时、结算、持久化、审计。
- AI Agent：按本规格生成/重构代码与测试；高风险变更必须停下等待人工审批。

## 4. 功能规格

### AUTH-01 注册
- 用户名 3–16 位字母/数字/下划线，唯一。
- 密码 8–72 字节，必须同时包含字母和数字；库中只存 BCrypt（strength 12）。
- 弱密码/重复用户名必须被拒绝；注册接口按 IP 限流（5 次/10 分钟）。
- 验收：`RegisterFormValidationTest`、`UserServiceTest`、`SecurityIntegrationTest.registerIsRateLimited`。

### AUTH-02 登录 / 退出
- 表单登录；失败 5 次按“用户名+IP”锁定 15 分钟；成功清零。
- 会话 Cookie：HttpOnly、SameSite=Lax、30 分钟超时；登录后 session fixation 防护。
- 未登录访问受保护页面跳登录；`/api/**` 未登录返回 401。
- 验收：`LoginAttemptServiceTest`、`SecurityIntegrationTest`、`PageRenderTest`。

### GAME-01 对局
- 15×15，黑先，自由五子棋（无禁手）；五连及以上判胜；满盘判和。
- 服务端校验：状态、轮次、坐标、空位、seq；客户端不做乐观落子。
- 每步 30 秒，超时判对手胜；认输立即结算。
- 验收：`BoardTest`、`GameServiceTest`、`GameWebSocketIntegrationTest`。

### GAME-02 匹配
- 内存队列，按入队时间排序；评分差从 100 开始，每 5 秒放宽 50，上限 400。
- 同一用户不可重复入队；对局中不可再匹配；双方 RESUME 后进入 PLAYING。
- 验收：`MatchmakingService` 逻辑 + WebSocket 集成测试（QUEUE_JOIN→MATCH_FOUND）。

### GAME-03 断线 / 重连
- 掉线通知对手；60 秒宽限；期间 RESUME 恢复棋盘、轮次、聊天，并通知对手。
- 超时判离线方负；双方都超时判 ABORT，不计分。
- 验收：`GameServiceTest.disconnectBeyondGraceLoses`、`reconnectWithinGraceKeepsGameAlive`。

### CHAT-01 对局聊天
- 仅本局两名玩家；≤200 字；1 条/秒；落库并广播；历史最近 50 条随 STATE 下发。
- 前端用 `textContent` 渲染，禁止 `innerHTML`。
- 验收：`ChatServiceTest`、`GameWebSocketIntegrationTest`（双端收到 CHAT、落库 1 条）。

### RATING-01 ELO
- 初始 1200；`E=1/(1+10^((Rb-Ra)/400))`；`Δ=K×(S−E)`；K=32（<30 局）/16（≥30 局）；下限 100。
- 胜/负/平更新评分与 `rating_history`；ABORT 不计分。
- 验收：`EloRatingServiceTest`、`GameServiceTest.blackWinsAfterFiveInARow`。

### RANK-01 排行榜
- 页面 `/leaderboard` 与接口 `/api/leaderboard`；Top 50；我的排名；同分并列。
- 验收：`LeaderboardServiceTest`；页面手工检查。

## 5. 非功能规格

- **性能目标**：单实例 200 WS 连接；落子端到端 p95 < 100 ms（局域网）；榜单 p95 < 30 ms。
  当前仅完成复杂度分析与本机 HTTP 基线（p95 65 ms），WS 压测待目标环境执行。
- **可用性**：断线 60 秒内可恢复；超时自动结算，避免永久卡局。
- **可维护性**：按功能分包；规则可单测；Flyway 管 schema；配置外置。
- **兼容性**：Chrome/Edge 现代浏览器；Canvas + 原生 ES Module；窄屏可用。

## 6. 协议约束

- 端点 `/ws/game`；握手支持会话 Cookie 或 5 分钟 ticket。
- 信封 `{op, seq, data}`；单条消息 ≤4KB；同一用户只保留一条活跃连接。
- 客户端→服务端：`QUEUE_JOIN/QUEUE_LEAVE/RESUME/MOVE/CHAT/RESIGN/PING`。
- 服务端→客户端：`QUEUED/MATCH_FOUND/STATE/MOVE/CHAT/GAME_OVER/OPPONENT_LEFT/OPPONENT_BACK/ERROR/PONG`。
- 广播必须在事务提交后执行；错误码：`NOT_YOUR_TURN/INVALID_MOVE/SEQ_MISMATCH/GAME_OVER/NOT_IN_GAME/RATE_LIMITED`。

## 7. 数据约束

- `users.username` 唯一；`users.version` 乐观锁；`rating` 建索引。
- `moves(match_id, seq)` 唯一；棋盘由 moves 重放。
- 外键：对局→用户、落子/聊天/流水→对局；删除顺序必须先子后父。
- 只允许通过 Flyway 修改 schema，禁止 `ddl-auto=update`。

## 8. 安全红线（不满足即拒绝合入）

1. 密码不得明文/弱哈希；不得记录明文密码。
2. 不得关闭 CSRF；不得把 JWT/token 放 localStorage。
3. WebSocket 必须鉴权；未认证连接必须关闭。
4. 聊天/昵称必须转义；禁止 innerHTML。
5. 对局页、WS 操作必须校验参与者，禁止越权。
6. 不得把数据库口令写进源码；使用环境变量。
7. 高风险变更（认证、迁移、评分、安全头）必须人工审批。

## 9. 测试映射

| 规格 | 测试类 | 层级 |
| --- | --- | --- |
| AUTH-01/02 | RegisterFormValidationTest, UserServiceTest, LoginAttemptServiceTest, SecurityIntegrationTest | L0/L1 |
| GAME-01 | BoardTest, GameServiceTest, GameWebSocketIntegrationTest | L0/L2 |
| GAME-02/03 | GameServiceTest, GameWebSocketIntegrationTest | L0/L2 |
| CHAT-01 | ChatServiceTest, GameWebSocketIntegrationTest | L0/L2 |
| RATING-01/RANK-01 | EloRatingServiceTest, LeaderboardServiceTest, GameServiceTest | L0/L1 |

## 10. AI 使用流程

1. 读取本规格与对应验收标准。
2. 生成最小变更；先补测试再改实现。
3. 运行 `mvn test`；新增用例必须可复现。
4. 高风险变更进入人工审批门禁（见 `docs/ai/approval-gates.md`）。
5. 记录 prompt、产出、人工修改与测试结果（见 `docs/ai/prompt-log.md`）。
6. 若测试失败或边界不清，回到需求重新定义，而不是绕过测试。
