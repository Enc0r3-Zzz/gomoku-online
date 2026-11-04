# 五子棋在线对战（Gomoku Online）

基于 **Spring Boot 3 + Java 17 + MySQL 8 + Spring Data JPA** 的在线五子棋课程项目。
页面用 Thymeleaf + 原生 JS，实时对局走 WebSocket，服务端权威裁决。

## 当前进度

| 阶段 | 内容 | 状态 |
| --- | --- | --- |
| M0 | 工程骨架、Flyway 建表、Security、登录/注册/大厅 | ✅ |
| M1 | 登录锁定、限流、安全加固 | ✅ |
| M2 | 棋局引擎、WebSocket、匹配、落子 | ✅ |
| M3 | 聊天、计时、认输、断线重连 | ⬜ |
| M4 | ELO、榜单、结算 | ⬜ |
| M5 | 棋盘 Canvas、页面打磨 | ⬜ |
| M6 | 压测、打包、部署 | ⬜ |
| M7 | 报告、PPT、AI 文档 | ⬜ |

## 技术栈

- Java 17（Temurin）
- Spring Boot 3.5.7 / Spring Security 6 / Spring Data JPA
- MySQL 8.0 + Flyway
- Thymeleaf + 原生 ES Module + Canvas
- Maven 3.9

## 快速开始

### 1. 环境

| 依赖 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 17 | 本机：`D:\ProgramFiles\DevTools\Java\jdk` |
| Maven | 3.9+ | 已配置阿里云镜像（`~/.m2/settings.xml`） |
| MySQL | 8.0 | 本机 3306 已运行 |

### 2. 建库（只需一次）

用 root 执行：

```bash
mysql -u root -p < scripts/setup-db.sql
```

或在 MySQL Workbench 中打开 `scripts/setup-db.sql` 执行。

默认连接：`gomoku / gomoku_dev_2026`，可用环境变量覆盖：

```
DB_URL / DB_USERNAME / DB_PASSWORD
```

### 3. 启动

Windows：

```bat
scripts\dev.bat
```

脚本会自动选择 JDK 17（优先 `JAVA17_HOME`，其次本机 `D:\ProgramFiles\DevTools\Java\jdk`，最后 `JAVA_HOME`），
即使系统 `JAVA_HOME` 指向 JDK 24 也能正确使用 17。只检查环境、不启动应用：

```bat
scripts\dev.bat --check
```

或手动：

```bash
set JAVA_HOME=D:\ProgramFiles\DevTools\Java\jdk
mvn spring-boot:run
```

访问 <http://localhost:8080> → 自动跳转登录页。

### 4. 测试

```bash
mvn test
```

## M0 验收清单

- [ ] `mvn test` 全绿（密码哈希、注册校验、重复用户名）
- [ ] 启动后 Flyway 自动创建 5 张表：`users`、`game_matches`、`moves`、`chat_messages`、`rating_history`
- [ ] `/login` 页面正常显示
- [ ] 注册：正常账号成功；重复用户名提示「已被占用」；弱密码提示规则
- [ ] 登录成功进入 `/lobby`，显示昵称、评分 1200、胜负 0
- [ ] 退出登录后回到登录页，受保护页面重新跳登录
- [ ] 密码在库中为 BCrypt 哈希（`$2a$12$...`）

## 目录结构

```
src/main/java/com/example/gomoku/
├── config/       # Security 配置
├── security/     # UserDetailsService
├── user/         # 实体、注册/登录、大厅
└── common/       # 异常与错误码

src/main/resources/
├── db/migration/ # Flyway SQL
├── templates/    # Thymeleaf 页面
└── static/       # CSS/JS

docs/             # 需求、方案、验证方案
scripts/          # 建库脚本、启动脚本
```

## 设计文档

- [可行性评估](docs/五子棋项目可行性评估.md)
- [项目实现方案](docs/项目实现方案.md)
- [初期验证方案](docs/初期验证方案.md)
