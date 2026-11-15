from PIL import Image, ImageDraw, ImageFont
import os

OUT = r"D:\java project\docs\报告\assets"
os.makedirs(OUT, exist_ok=True)

INK = (43, 38, 32)
MUTED = (123, 114, 100)
ACCENT = (138, 90, 43)
ACCENT_DARK = (111, 70, 32)
ACCENT_SOFT = (241, 227, 207)
CARD = (255, 253, 248)
LINE = (216, 203, 181)
BG = (247, 243, 234)
WHITE = (255, 255, 255)

FONT = "C:/Windows/Fonts/msyh.ttc"
FONT_BOLD = "C:/Windows/Fonts/msyhbd.ttc"


def font(size, bold=False):
    return ImageFont.truetype(FONT_BOLD if bold else FONT, size)


def text_center(draw, cx, cy, text, f, fill=INK, spacing=6):
    bbox = draw.multiline_textbbox((0, 0), text, font=f, align="center", spacing=spacing)
    w = bbox[2] - bbox[0]
    h = bbox[3] - bbox[1]
    draw.multiline_text((cx - w / 2, cy - h / 2), text, font=f, fill=fill,
                        align="center", spacing=spacing)


def arrow_down(draw, x, y1, y2, color=ACCENT, width=4):
    draw.line((x, y1, x, y2), fill=color, width=width)
    draw.polygon([(x, y2), (x - 10, y2 - 16), (x + 10, y2 - 16)], fill=color)


def arrow_right(draw, x1, x2, y, color=ACCENT, width=3, dashed=False):
    if dashed:
        step = 14
        for x in range(x1, x2, step * 2):
            draw.line((x, y, min(x + step, x2), y), fill=color, width=width)
    else:
        draw.line((x1, y, x2, y), fill=color, width=width)
    draw.polygon([(x2, y), (x2 - 14, y - 8), (x2 - 14, y + 8)], fill=color)


def box(draw, xy, fill=CARD, outline=LINE, radius=16, width=2):
    draw.rounded_rectangle(xy, radius=radius, fill=fill, outline=outline, width=width)


def architecture():
    W, H = 1600, 1000
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)

    text_center(d, W / 2, 48, "系统总体架构", font(44, True), INK)

    # browser layer
    box(d, (90, 100, 1510, 270), CARD)
    d.text((120, 118), "浏览器（Thymeleaf + 原生 JS）", font=font(26, True), fill=ACCENT_DARK)
    items = [
        ("页面模板\nlogin / lobby / game / leaderboard", 130, 165, 430),
        ("WebSocket 客户端\nws.js：连接 / 心跳 / 重连", 585, 165, 430),
        ("Canvas 棋盘\nboard.js：网格 / 棋子 / 连线", 1040, 165, 430),
    ]
    for text, x, y, w in items:
        box(d, (x, y, x + w, y + 82), ACCENT_SOFT, (230, 207, 174), radius=12)
        text_center(d, x + w / 2, y + 41, text, font(20), INK)

    arrow_down(d, W / 2, 270, 330)
    d.text((W / 2 + 22, 282), "HTTPS / WSS", font=font(20, True), fill=MUTED)

    # spring layer
    box(d, (90, 330, 1510, 720), CARD)
    d.text((120, 348), "Spring Boot 3.5.7 单进程", font=font(26, True), fill=ACCENT_DARK)
    modules = [
        ("Spring Security\n会话 / CSRF / 限流", 130, 405),
        ("WebSocket 网关\n握手鉴权 / 协议编解码", 585, 405),
        ("对局引擎\n落子 / 胜负 / 计时", 1040, 405),
        ("匹配服务\n队列 / 评分分段", 130, 545),
        ("评分与榜单\nELO / 流水 / Top 50", 585, 545),
        ("聊天服务\n校验 / 落库 / 广播", 1040, 545),
    ]
    for text, x, y in modules:
        box(d, (x, y, x + 430, y + 92), WHITE, LINE, radius=12)
        text_center(d, x + 215, y + 46, text, font(20), INK)

    arrow_down(d, W / 2, 720, 785)
    d.text((W / 2 + 22, 732), "JPA / Flyway", font=font(20, True), fill=MUTED)

    # db layer
    box(d, (90, 785, 1510, 950), CARD)
    d.text((120, 803), "MySQL 8.0（5 张业务表）", font=font(26, True), fill=ACCENT_DARK)
    tables = ["users", "game_matches", "moves", "chat_messages", "rating_history"]
    widths = [250, 270, 190, 280, 280]
    x = 130
    for name, w in zip(tables, widths):
        box(d, (x, 850, x + w, 910), ACCENT_SOFT, (230, 207, 174), radius=10)
        text_center(d, x + w / 2, 880, name, font(19), INK)
        x += w + 18

    img.save(os.path.join(OUT, "architecture.png"), quality=95)


def table_box(d, x, y, w, title, rows, row_h=34, header_h=48):
    total_h = header_h + row_h * len(rows)
    box(d, (x, y, x + w, y + total_h), CARD)
    d.rounded_rectangle((x, y, x + w, y + header_h), radius=14, fill=ACCENT)
    d.rectangle((x, y + header_h - 14, x + w, y + header_h), fill=ACCENT)
    text_center(d, x + w / 2, y + header_h / 2, title, font(20, True), WHITE)
    for i, row in enumerate(rows):
        cy = y + header_h + row_h * i + row_h / 2
        d.text((x + 16, cy - 10), row, font=font(16), fill=INK)
        if i:
            d.line((x + 10, y + header_h + row_h * i, x + w - 10,
                    y + header_h + row_h * i), fill=(238, 232, 220), width=1)
    return total_h


def er():
    W, H = 1600, 1000
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    text_center(d, W / 2, 44, "数据库 ER 关系", font(44, True), INK)

    users = [
        "id  BIGINT PK",
        "username  VARCHAR(32) UNIQUE",
        "password_hash  VARCHAR(100)",
        "nickname  VARCHAR(32)",
        "rating  INT",
        "wins / losses / draws  INT",
        "version  BIGINT",
    ]
    matches = [
        "id  BIGINT PK",
        "black_id  FK -> users",
        "white_id  FK -> users",
        "status  WAITING/PLAYING/...",
        "result  BLACK_WIN/...",
        "winner_id  FK -> users",
        "started_at / ended_at",
        "version  BIGINT",
    ]
    moves = [
        "id  BIGINT PK",
        "match_id  FK -> game_matches",
        "seq  INT  UNIQUE(match_id, seq)",
        "player_id  FK -> users",
        "x / y  INT",
        "created_at  DATETIME",
    ]
    chats = [
        "id  BIGINT PK",
        "match_id  FK -> game_matches",
        "sender_id  FK -> users",
        "content  VARCHAR(200)",
        "created_at  DATETIME",
    ]
    ratings = [
        "id  BIGINT PK",
        "user_id  FK -> users",
        "match_id  FK -> game_matches",
        "rating_before / after",
        "delta  INT",
        "created_at  DATETIME",
    ]

    table_box(d, 70, 190, 390, "users", users)
    table_box(d, 600, 170, 430, "game_matches", matches)
    table_box(d, 1140, 100, 390, "moves", moves)
    table_box(d, 1140, 390, 390, "chat_messages", chats)
    table_box(d, 1140, 680, 390, "rating_history", ratings)

    # connectors
    arrow_right(d, 460, 590, 300, ACCENT, 3)
    d.text((490, 272), "black/white", font=font(15), fill=MUTED)

    arrow_right(d, 1030, 1130, 200, ACCENT, 3)
    d.text((1048, 172), "1:N", font=font(15), fill=MUTED)
    arrow_right(d, 1030, 1130, 450, ACCENT, 3)
    d.text((1048, 422), "1:N", font=font(15), fill=MUTED)
    arrow_right(d, 1030, 1130, 700, ACCENT, 3)
    d.text((1048, 672), "1:N", font=font(15), fill=MUTED)

    # users -> rating_history (orthogonal)
    d.line((460, 470, 520, 470), fill=ACCENT, width=3)
    d.line((520, 470, 520, 860), fill=ACCENT, width=3)
    d.line((520, 860, 1130, 860), fill=ACCENT, width=3)
    d.polygon([(1130, 860), (1116, 852), (1116, 868)], fill=ACCENT)
    d.text((540, 840), "user_id", font=font(15), fill=MUTED)

    img.save(os.path.join(OUT, "er.png"), quality=95)


def board_image():
    S = 800
    pad = 44
    cell = (S - 2 * pad) / 14

    img = Image.new("RGB", (S, S), (210, 164, 95))
    d = ImageDraw.Draw(img)
    for y in range(S):
        t = y / S
        r = int(230 + (210 - 230) * t)
        g = int(192 + (164 - 192) * t)
        b = int(123 + (95 - 123) * t)
        d.line((0, y, S, y), fill=(r, g, b))

    grid = (70, 45, 15)
    for i in range(15):
        at = pad + i * cell
        d.line((pad, at, S - pad, at), fill=grid, width=1)
        d.line((at, pad, at, S - pad), fill=grid, width=1)

    stars = [(3, 3), (11, 3), (3, 11), (11, 11), (7, 7)]
    for x, y in stars:
        cx, cy = pad + x * cell, pad + y * cell
        d.ellipse((cx - 4, cy - 4, cx + 4, cy + 4), fill=(60, 38, 12))

    def stone(x, y, black):
        cx, cy = pad + x * cell, pad + y * cell
        r = cell * 0.42
        d.ellipse((cx - r + 2, cy - r + 3, cx + r + 2, cy + r + 3),
                  fill=(120, 95, 60))
        if black:
            d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=(17, 17, 17))
            d.ellipse((cx - r * 0.45, cy - r * 0.55, cx + r * 0.05, cy - r * 0.05),
                      fill=(95, 95, 95))
        else:
            d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=(245, 243, 236))
            d.ellipse((cx - r * 0.45, cy - r * 0.55, cx + r * 0.05, cy - r * 0.05),
                      fill=(255, 255, 255))

    moves = [
        (7, 7, True), (7, 8, False),
        (6, 7, True), (6, 8, False),
        (8, 7, True), (8, 8, False),
        (5, 7, True), (7, 9, False),
        (9, 7, True),
    ]
    for x, y, black in moves:
        stone(x, y, black)

    # winning line
    x1, y1 = pad + 5 * cell, pad + 7 * cell
    x2, y2 = pad + 9 * cell, pad + 7 * cell
    d.line((x1, y1, x2, y2), fill=(192, 57, 43), width=7)
    # last move ring
    cx, cy = pad + 9 * cell, pad + 7 * cell
    d.ellipse((cx - cell * 0.24, cy - cell * 0.24, cx + cell * 0.24, cy + cell * 0.24),
              outline=(192, 57, 43), width=3)

    img.save(os.path.join(OUT, "board.png"), quality=95)


def sequence():
    W, H = 1600, 900
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    text_center(d, W / 2, 42, "匹配与对局时序", font(42, True), INK)

    xs = {"a": 250, "s": 800, "b": 1350}
    top, bottom = 150, 840
    for key, label, color in [("a", "玩家 A", ACCENT), ("s", "服务器", INK), ("b", "玩家 B", ACCENT)]:
        x = xs[key]
        box(d, (x - 90, 100, x + 90, 150), ACCENT_SOFT, (230, 207, 174), radius=10)
        text_center(d, x, 125, label, font(19, True), color)
        for y in range(top, bottom, 18):
            d.line((x, y, x, y + 9), fill=(180, 165, 140), width=2)

    msgs = [
        ("a", "s", 205, "QUEUE_JOIN", ACCENT),
        ("b", "s", 245, "QUEUE_JOIN", ACCENT),
        ("s", "a", 305, "MATCH_FOUND", ACCENT_DARK),
        ("s", "b", 305, "MATCH_FOUND", ACCENT_DARK),
        ("a", "s", 365, "RESUME", ACCENT),
        ("b", "s", 405, "RESUME", ACCENT),
        ("s", "a", 465, "STATE (PLAYING)", ACCENT_DARK),
        ("s", "b", 465, "STATE (PLAYING)", ACCENT_DARK),
        ("a", "s", 525, "MOVE seq=1", ACCENT),
        ("s", "b", 565, "MOVE 广播", ACCENT_DARK),
        ("b", "s", 625, "CHAT", ACCENT),
        ("s", "a", 665, "CHAT 广播", ACCENT_DARK),
        ("a", "s", 725, "MOVE（五连）", ACCENT),
        ("s", "a", 785, "GAME_OVER + deltas", (160, 60, 48)),
        ("s", "b", 785, "GAME_OVER + deltas", (160, 60, 48)),
    ]

    for src, dst, y, label, color in msgs:
        x1, x2 = xs[src], xs[dst]
        if x1 < x2:
            start, end = x1 + 6, x2 - 6
        else:
            start, end = x1 - 6, x2 + 6
        d.line((start, y, end, y), fill=color, width=3)
        d.polygon([(end, y), (end - 14 * (1 if x1 < x2 else -1), y - 8),
                   (end - 14 * (1 if x1 < x2 else -1), y + 8)], fill=color)
        mid = (x1 + x2) / 2
        text_center(d, mid, y - 18, label, font(15), MUTED)

    img.save(os.path.join(OUT, "sequence.png"), quality=95)


architecture()
er()
board_image()
sequence()
print("assets generated")
