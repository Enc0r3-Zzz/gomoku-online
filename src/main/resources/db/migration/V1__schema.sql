-- 五子棋在线对战 · 初始 schema
-- MySQL 8.0 / InnoDB / utf8mb4

CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(32)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    nickname      VARCHAR(32)  NOT NULL,
    rating        INT          NOT NULL DEFAULT 1200,
    wins          INT          NOT NULL DEFAULT 0,
    losses        INT          NOT NULL DEFAULT 0,
    draws         INT          NOT NULL DEFAULT 0,
    created_at    DATETIME(6)  NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    KEY idx_users_rating (rating DESC)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE game_matches (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    black_id   BIGINT      NOT NULL,
    white_id   BIGINT      NOT NULL,
    status     VARCHAR(16) NOT NULL,
    result     VARCHAR(16) NULL,
    winner_id  BIGINT      NULL,
    started_at DATETIME(6) NOT NULL,
    ended_at   DATETIME(6) NULL,
    version    BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_matches_black (black_id),
    KEY idx_matches_white (white_id),
    KEY idx_matches_status (status),
    CONSTRAINT fk_matches_black FOREIGN KEY (black_id) REFERENCES users (id),
    CONSTRAINT fk_matches_white FOREIGN KEY (white_id) REFERENCES users (id),
    CONSTRAINT fk_matches_winner FOREIGN KEY (winner_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE moves (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    match_id   BIGINT      NOT NULL,
    seq        INT         NOT NULL,
    player_id  BIGINT      NOT NULL,
    x          TINYINT     NOT NULL,
    y          TINYINT     NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_moves_match_seq (match_id, seq),
    CONSTRAINT fk_moves_match FOREIGN KEY (match_id) REFERENCES game_matches (id) ON DELETE CASCADE,
    CONSTRAINT fk_moves_player FOREIGN KEY (player_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE chat_messages (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    match_id   BIGINT       NOT NULL,
    sender_id  BIGINT       NOT NULL,
    content    VARCHAR(200) NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_chat_match (match_id, id),
    CONSTRAINT fk_chat_match FOREIGN KEY (match_id) REFERENCES game_matches (id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_sender FOREIGN KEY (sender_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE rating_history (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    user_id       BIGINT      NOT NULL,
    match_id      BIGINT      NOT NULL,
    rating_before INT         NOT NULL,
    rating_after  INT         NOT NULL,
    delta         INT         NOT NULL,
    created_at    DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_rating_user (user_id, created_at),
    CONSTRAINT fk_rating_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_rating_match FOREIGN KEY (match_id) REFERENCES game_matches (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
