-- 坐标使用 INT，避免 TINYINT 与 Java int 的 schema 校验不一致
ALTER TABLE moves
    MODIFY COLUMN x INT NOT NULL,
    MODIFY COLUMN y INT NOT NULL;
