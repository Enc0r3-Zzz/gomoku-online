# -*- coding: utf-8 -*-
import os
from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.enum.shapes import MSO_SHAPE
from pptx.oxml.ns import qn

BASE = r"D:\java project"
ASSETS = os.path.join(BASE, "docs", "报告", "assets")
OUT = os.path.join(BASE, "docs", "报告", "五子棋项目答辩.pptx")

BG = RGBColor(0xF7, 0xF3, 0xEA)
CARD = RGBColor(0xFF, 0xFD, 0xF8)
INK = RGBColor(0x2B, 0x26, 0x20)
MUTED = RGBColor(0x7B, 0x72, 0x64)
ACCENT = RGBColor(0x8A, 0x5A, 0x2B)
ACCENT2 = RGBColor(0xC9, 0x8A, 0x3D)
SOFT = RGBColor(0xF1, 0xE3, 0xCF)
LINE = RGBColor(0xD8, 0xCB, 0xB5)
WHITE = RGBColor(0xFF, 0xFF, 0xFF)
DARK = RGBColor(0x3A, 0x2A, 0x1C)

TOTAL = 12


def set_run(run, size=18, bold=False, color=INK, name="Microsoft YaHei"):
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.color.rgb = color
    run.font.name = name
    rPr = run._r.get_or_add_rPr()
    ea = rPr.find(qn("a:ea"))
    if ea is None:
        ea = rPr.makeelement(qn("a:ea"), {})
        rPr.append(ea)
    ea.set("typeface", name)


def bg(slide):
    shape = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0,
                                   Inches(13.333), Inches(7.5))
    shape.fill.solid()
    shape.fill.fore_color.rgb = BG
    shape.line.fill.background()
    shape.shadow.inherit = False


def text(slide, x, y, w, h, value, size=18, bold=False, color=INK,
         align=PP_ALIGN.LEFT, anchor=MSO_ANCHOR.TOP, line=1.1):
    box = slide.shapes.add_textbox(Inches(x), Inches(y), Inches(w), Inches(h))
    tf = box.text_frame
    tf.word_wrap = True
    tf.margin_left = 0
    tf.margin_right = 0
    tf.margin_top = 0
    tf.margin_bottom = 0
    tf.vertical_anchor = anchor
    p = tf.paragraphs[0]
    p.alignment = align
    p.line_spacing = line
    run = p.add_run()
    run.text = value
    set_run(run, size, bold, color)
    return box


def bullets(slide, x, y, w, h, items, size=14, color=INK, gap=6):
    box = slide.shapes.add_textbox(Inches(x), Inches(y), Inches(w), Inches(h))
    tf = box.text_frame
    tf.word_wrap = True
    tf.margin_left = 0
    tf.margin_right = 0
    tf.margin_top = 0
    tf.margin_bottom = 0
    for i, item in enumerate(items):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.line_spacing = 1.15
        p.space_after = Pt(gap)
        run = p.add_run()
        run.text = "•  " + item
        set_run(run, size, False, color)
    return box


def card(slide, x, y, w, h, fill=CARD, line=LINE, radius=0.06):
    shape = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE,
                                   Inches(x), Inches(y), Inches(w), Inches(h))
    shape.fill.solid()
    shape.fill.fore_color.rgb = fill
    shape.line.color.rgb = line
    shape.line.width = Pt(0.75)
    shape.shadow.inherit = False
    try:
        shape.adjustments[0] = radius
    except Exception:
        pass
    return shape


def title(slide, text_value, subtitle=None):
    bar = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE,
                                 Inches(0.7), Inches(0.55), Inches(0.12), Inches(0.55))
    bar.fill.solid()
    bar.fill.fore_color.rgb = ACCENT
    bar.line.fill.background()
    bar.shadow.inherit = False
    text(slide, 0.95, 0.42, 11.6, 0.8, text_value, 28, True, INK)
    if subtitle:
        text(slide, 0.96, 1.05, 11.6, 0.4, subtitle, 12, False, MUTED)


def footer(slide, index):
    text(slide, 0.7, 7.12, 6.0, 0.25, "五子棋在线对战平台 · Java语言及网络编程", 9, False, MUTED)
    text(slide, 11.7, 7.12, 0.9, 0.25, f"{index:02d} / {TOTAL}", 9, False, MUTED,
         align=PP_ALIGN.RIGHT)


def kpi(slide, x, y, w, h, value, label):
    card(slide, x, y, w, h)
    text(slide, x + 0.2, y + 0.22, w - 0.4, 0.7, value, 34, True, ACCENT)
    text(slide, x + 0.2, y + h - 0.55, w - 0.4, 0.3, label, 11, False, MUTED)


def new_slide(prs):
    slide = prs.slides.add_slide(prs.slide_layouts[6])
    bg(slide)
    return slide


def main():
    prs = Presentation()
    prs.slide_width = Inches(13.333)
    prs.slide_height = Inches(7.5)

    # 1 cover
    s = new_slide(prs)
    band = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, Inches(0.22), Inches(7.5))
    band.fill.solid()
    band.fill.fore_color.rgb = ACCENT
    band.line.fill.background()
    text(s, 0.85, 0.7, 7.0, 0.4, "JAVA语言及网络编程 · 课程报告", 12, True, ACCENT)
    text(s, 0.85, 1.45, 7.4, 1.6, "五子棋在线对战平台", 40, True, INK)
    text(s, 0.85, 2.95, 7.4, 0.6, "设计与实现", 26, False, ACCENT)
    text(s, 0.85, 3.75, 7.2, 0.8,
         "Spring Boot 3 + WebSocket + MySQL 8 + ELO", 15, False, MUTED)
    text(s, 0.85, 5.3, 7.0, 0.4, "姓名：____________    学号：____________", 14, False, INK)
    text(s, 0.85, 5.8, 7.0, 0.4, "专业：____________    任课教师：____________", 14, False, INK)
    text(s, 0.85, 6.65, 7.0, 0.35, "2026 年 11 月", 12, False, MUTED)
    board = s.shapes.add_picture(os.path.join(ASSETS, "board.png"),
                                 Inches(8.55), Inches(0.95), width=Inches(4.15))
    board.shadow.inherit = False

    # 2 overview
    s = new_slide(prs)
    title(s, "项目概览", "从需求到可运行系统：六个阶段、58 个自动化用例")
    kpi(s, 0.7, 1.55, 2.75, 1.45, "6", "核心模块")
    kpi(s, 3.75, 1.55, 2.75, 1.45, "58", "自动化用例")
    kpi(s, 6.8, 1.55, 2.75, 1.45, "5", "数据库表")
    kpi(s, 9.85, 1.55, 2.75, 1.45, "11", "Git 提交")
    card(s, 0.7, 3.45, 7.6, 3.2)
    text(s, 1.0, 3.7, 7.0, 0.4, "系统能力", 16, True, ACCENT)
    bullets(s, 1.0, 4.2, 6.9, 2.2, [
        "服务端权威：落子、轮次、胜负、计时全部由服务端裁决",
        "实时对局：匹配、落子、聊天共用一条 WebSocket",
        "断线恢复：60 秒宽限，按 seq 重放 moves 恢复棋盘",
        "天梯排名：ELO 评分、评分流水、Top 50 与我的排名",
    ], 12.5)
    card(s, 8.65, 3.45, 3.95, 3.2)
    text(s, 8.95, 3.7, 3.3, 0.4, "技术栈", 16, True, ACCENT)
    for i, (name, ver) in enumerate([
        ("Java", "17 LTS"), ("Spring Boot", "3.5.7"), ("MySQL", "8.0.45"),
        ("Thymeleaf", "3.1"), ("WebSocket", "原生 Handler"),
    ]):
        y = 4.2 + i * 0.44
        text(s, 8.95, y, 1.7, 0.3, name, 11.5, True, INK)
        text(s, 10.6, y, 1.8, 0.3, ver, 11, False, MUTED)
    footer(s, 2)

    # 3 requirements
    s = new_slide(prs)
    title(s, "需求分析", "功能、非功能与安全三条线同时约束")
    cols = [
        ("功能需求", [
            "注册 / 登录 / 退出",
            "按评分匹配对手",
            "15×15 对局、五连判胜",
            "棋盘下方实时聊天",
            "ELO 天梯与排行榜",
            "断线重连 / 超时判负",
        ]),
        ("非功能需求", [
            "单实例 200 WS 连接（目标）",
            "落子 p95 < 100 ms（目标）",
            "60 秒内可重连",
            "规则可单测、schema 可迁移",
            "Chrome / Edge 可用",
            "响应式布局",
        ]),
        ("安全需求", [
            "BCrypt 12 存储密码",
            "会话 HttpOnly + SameSite",
            "CSRF token",
            "XSS 转义 + CSP",
            "登录锁定 + 注册限流",
            "WS 握手鉴权",
        ]),
    ]
    for i, (head, items) in enumerate(cols):
        x = 0.7 + i * 4.15
        card(s, x, 1.45, 3.85, 4.55)
        text(s, x + 0.25, 1.7, 3.3, 0.4, head, 17, True, ACCENT)
        bullets(s, x + 0.25, 2.25, 3.3, 3.5, items, 11.5, INK, 7)
    card(s, 0.7, 6.2, 12.0, 0.7, SOFT, SOFT)
    text(s, 0.95, 6.35, 11.6, 0.35,
         "边界：不做真 P2P、观战、禁手与多实例；P2P 作为方案对比，排位坚持服务端权威。",
         11.5, False, DARK)
    footer(s, 3)

    # 4 architecture
    s = new_slide(prs)
    title(s, "总体架构")
    pic = s.shapes.add_picture(os.path.join(ASSETS, "architecture.png"),
                               Inches(1.1), Inches(1.18), width=Inches(9.6))
    pic.shadow.inherit = False
    text(s, 11.0, 2.0, 2.0, 0.4, "关键点", 14, True, ACCENT)
    bullets(s, 11.0, 2.45, 1.95, 3.8, [
        "浏览器只渲染",
        "服务端做裁决",
        "Moves 可重放",
        "事务后广播",
    ], 10.5, MUTED, 8)
    footer(s, 4)

    # 5 authority + sequence
    s = new_slide(prs)
    title(s, "关键技术：服务端权威对局", "客户端不乐观落子，收到广播后才更新棋盘")
    pic = s.shapes.add_picture(os.path.join(ASSETS, "sequence.png"),
                               Inches(0.55), Inches(1.3), width=Inches(8.7))
    pic.shadow.inherit = False
    card(s, 9.55, 1.35, 3.15, 5.3)
    text(s, 9.8, 1.55, 2.7, 0.35, "一致性设计", 14, True, ACCENT)
    bullets(s, 9.8, 2.0, 2.65, 4.3, [
        "每步写入 moves，seq 唯一",
        "落子合法性由服务端校验",
        "胜负只围绕最后一手判断",
        "广播在事务提交后执行",
        "30 秒计时，超时判负",
    ], 10.5, INK, 9)
    footer(s, 5)

    # 6 match + elo
    s = new_slide(prs)
    title(s, "匹配与 ELO", "等待越久，分段越宽；评分只认服务端结果")
    card(s, 0.7, 1.5, 5.95, 4.85)
    text(s, 1.0, 1.75, 5.0, 0.4, "匹配规则", 18, True, ACCENT)
    text(s, 1.0, 2.35, 5.2, 1.1, "允许差值 = min(400,\n100 + 50 × floor(等待秒数 / 5))",
         16, False, INK, line=1.3)
    bullets(s, 1.0, 3.75, 5.2, 2.2, [
        "内存等待队列，按入队时间排序",
        "配对成功随机决定黑白",
        "双方 RESUME 后进入 PLAYING",
        "对局中不可重复入队",
    ], 11.5, MUTED, 8)
    card(s, 6.85, 1.5, 5.8, 4.85)
    text(s, 7.15, 1.75, 5.1, 0.4, "ELO 评分", 18, True, ACCENT)
    text(s, 7.15, 2.35, 5.1, 1.2, "E = 1 / (1 + 10^((Rb−Ra)/400))\nΔ = K × (S − E)",
         17, False, INK, line=1.3)
    bullets(s, 7.15, 3.75, 5.1, 2.2, [
        "初始 1200 分，下限 100",
        "K=32（<30 局）/ 16（≥30 局）",
        "平局按 0.5 计；ABORT 不计分",
        "rating_history 记录每一笔变化",
    ], 11.5, MUTED, 8)
    footer(s, 6)

    # 7 database
    s = new_slide(prs)
    title(s, "数据库设计", "5 张表覆盖用户、对局、落子、聊天与评分")
    pic = s.shapes.add_picture(os.path.join(ASSETS, "er.png"),
                               Inches(0.55), Inches(1.25), width=Inches(9.35))
    pic.shadow.inherit = False
    card(s, 10.1, 1.4, 2.7, 5.4)
    text(s, 10.35, 1.6, 2.2, 0.35, "设计要点", 14, True, ACCENT)
    bullets(s, 10.35, 2.05, 2.2, 4.5, [
        "moves 是重连依据",
        "seq 唯一保证顺序",
        "rating 索引供榜单",
        "version 乐观锁",
        "Flyway 管理迁移",
    ], 10, INK, 9)
    footer(s, 7)

    # 8 UI
    s = new_slide(prs)
    title(s, "界面展示", "暖木色棋盘 + 棋盘下方聊天栏 + 响应式布局")
    board = s.shapes.add_picture(os.path.join(ASSETS, "board.png"),
                                 Inches(0.7), Inches(1.35), width=Inches(4.1))
    board.shadow.inherit = False
    login = s.shapes.add_picture(os.path.join(ASSETS, "login_crop.png"),
                                 Inches(5.15), Inches(1.35), width=Inches(2.55))
    login.shadow.inherit = False
    card(s, 8.0, 1.35, 4.6, 5.0)
    text(s, 8.3, 1.6, 4.0, 0.4, "交互细节", 16, True, ACCENT)
    bullets(s, 8.3, 2.1, 4.0, 4.0, [
        "Canvas 棋盘：木纹、星位、最后一手",
        "悬停半透明预落子",
        "胜利连线高亮",
        "落点与鼠标精确重合",
        "聊天在棋盘正下方",
    ], 11.5, INK, 10)
    footer(s, 8)

    # 9 verification
    s = new_slide(prs)
    title(s, "系统验证", "58 个用例全部通过，含真实双客户端完整对局")
    layers = [
        ("L3 / L4", "双浏览器 / 局域网手工验证", 3.3, SOFT),
        ("L2", "WebSocket 双客户端：1 个完整对局", 4.8, RGBColor(0xE9, 0xD5, 0xB9)),
        ("L1", "认证 / 榜单 / 安全 / 页面：约 20 个", 6.3, RGBColor(0xDD, 0xC4, 0xA2)),
        ("L0", "规则 / ELO / 校验：约 37 个", 7.8, RGBColor(0xD1, 0xB3, 0x8C)),
    ]
    for i, (tag, desc, width, color) in enumerate(layers):
        x = 0.8 + (7.8 - width) / 2
        y = 1.55 + i * 0.78
        shape = s.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE,
                                   Inches(x), Inches(y), Inches(width), Inches(0.62))
        shape.fill.solid()
        shape.fill.fore_color.rgb = color
        shape.line.color.rgb = LINE
        shape.shadow.inherit = False
        text(s, x + 0.2, y + 0.14, width - 0.4, 0.35, f"{tag}  {desc}", 10.5, True, DARK)
    card(s, 9.3, 1.35, 3.3, 5.4)
    text(s, 9.55, 1.55, 2.8, 0.35, "验证数据", 14, True, ACCENT)
    text(s, 9.55, 2.0, 2.8, 0.8, "58 / 58", 30, True, ACCENT)
    text(s, 9.55, 2.75, 2.8, 0.3, "自动化用例通过", 11, False, MUTED)
    bullets(s, 9.55, 3.35, 2.8, 3.0, [
        "9 步完整对局断言",
        "聊天落库与双端广播",
        "掉线判负 / 重连",
        "本机 HTTP p95 65 ms",
    ], 10, INK, 8)
    footer(s, 9)

    # 10 security
    s = new_slide(prs)
    title(s, "安全设计", "认证、输入、授权、审计四层防线")
    items = [
        ("密码", "BCrypt 12 + 长度/复杂度"),
        ("会话", "HttpOnly + SameSite + 30 分钟"),
        ("CSRF", "表单 token + fetch header"),
        ("登录爆破", "5 次失败锁 15 分钟"),
        ("限流", "注册 5 次/10 分钟/IP"),
        ("WebSocket", "会话 Cookie + 5 分钟 ticket"),
        ("XSS", "转义 + textContent + CSP"),
        ("越权", "对局/WS 校验参与者"),
        ("审计", "登录/注册/锁定日志"),
    ]
    for i, (head, desc) in enumerate(items):
        col = i % 3
        row = i // 3
        x = 0.7 + col * 4.15
        y = 1.45 + row * 1.55
        card(s, x, y, 3.8, 1.3)
        text(s, x + 0.2, y + 0.15, 3.3, 0.3, head, 12, True, ACCENT)
        text(s, x + 0.2, y + 0.55, 3.3, 0.6, desc, 10.5, False, INK)
    footer(s, 10)

    # 11 AI engineering
    s = new_slide(prs)
    title(s, "AI 工程化实践", "规格 → 生成 → 测试 → 审批 → 回归")
    steps = [
        ("规格", "SKILL.md\n验收标准"),
        ("生成", "模块代码\n测试用例"),
        ("验证", "JUnit / MockMvc\nWS 双端"),
        ("审批", "认证 / 迁移\n评分逻辑"),
        ("回归", "58 用例\nGit 提交"),
    ]
    for i, (head, body) in enumerate(steps):
        x = 0.7 + i * 2.45
        card(s, x, 1.55, 2.05, 1.55)
        text(s, x + 0.15, 1.7, 1.75, 0.3, head, 14, True, ACCENT)
        text(s, x + 0.15, 2.1, 1.75, 0.8, body, 10, False, INK, line=1.1)
        if i < 4:
            arrow = s.shapes.add_shape(MSO_SHAPE.RIGHT_ARROW,
                                       Inches(x + 2.1), Inches(2.05), Inches(0.28), Inches(0.3))
            arrow.fill.solid()
            arrow.fill.fore_color.rgb = ACCENT2
            arrow.line.fill.background()
            arrow.shadow.inherit = False
    card(s, 0.7, 3.5, 7.3, 2.9)
    text(s, 1.0, 3.7, 6.6, 0.35, "AI 负责 / 人工负责", 14, True, ACCENT)
    text(s, 1.0, 4.15, 6.6, 0.5, "AI：样板代码、测试用例、文档初稿", 11.5, False, INK)
    text(s, 1.0, 4.65, 6.6, 0.5, "人工：边界定义、协议设计、缺陷定位、最终取舍", 11.5, False, INK)
    text(s, 1.0, 5.25, 6.6, 0.8,
         "缺陷：TINYINT/int schema、WS Cookie 不转发、Canvas 落点偏移", 10.5, False, MUTED)
    card(s, 8.35, 3.5, 4.25, 2.9)
    text(s, 8.6, 3.7, 3.7, 0.35, "产物", 14, True, ACCENT)
    bullets(s, 8.6, 4.15, 3.7, 2.0, [
        "skills/gomoku-spec/SKILL.md",
        "docs/ai/prompt-log.md",
        "docs/ai/approval-gates.md",
        "docs/ai/eval-report.md",
    ], 10, INK, 7)
    footer(s, 11)

    # 12 summary
    s = new_slide(prs)
    title(s, "总结与展望", "一个可运行、可测试、可部署的完整闭环")
    card(s, 0.7, 1.45, 3.85, 4.3)
    text(s, 0.95, 1.65, 3.3, 0.35, "成果", 15, True, ACCENT)
    bullets(s, 0.95, 2.1, 3.3, 3.3, [
        "6 个模块、58 个用例",
        "服务端权威与事务后广播",
        "ELO 天梯与聊天",
        "jar / Docker / 部署文档",
    ], 10.5, INK, 8)
    card(s, 4.75, 1.45, 3.85, 4.3)
    text(s, 5.0, 1.65, 3.3, 0.35, "不足", 15, True, ACCENT)
    bullets(s, 5.0, 2.1, 3.3, 3.3, [
        "单实例内存状态",
        "未做正式 WS 压测",
        "未实现观战 / 禁手",
        "P2P 仅方案对比",
    ], 10.5, INK, 8)
    card(s, 8.8, 1.45, 3.85, 4.3)
    text(s, 9.05, 1.65, 3.3, 0.35, "展望", 15, True, ACCENT)
    bullets(s, 9.05, 2.1, 3.3, 3.3, [
        "Redis / 多实例",
        "观战与回放",
        "赛季 / 段位",
        "休闲房 WebRTC",
    ], 10.5, INK, 8)
    text(s, 0.7, 6.05, 12.0, 0.7, "谢谢，欢迎提问", 26, True, ACCENT, PP_ALIGN.CENTER)
    footer(s, 12)

    prs.save(OUT)
    print("saved", OUT)


if __name__ == "__main__":
    main()
