-- 开发环境数据库初始化（用 root 执行一次）
-- 生产环境请换成强密码，并通过环境变量 DB_PASSWORD 注入

CREATE DATABASE IF NOT EXISTS gomoku
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'gomoku'@'localhost' IDENTIFIED BY 'gomoku_dev_2026';
ALTER USER 'gomoku'@'localhost' IDENTIFIED BY 'gomoku_dev_2026';

GRANT ALL PRIVILEGES ON gomoku.* TO 'gomoku'@'localhost';
FLUSH PRIVILEGES;
