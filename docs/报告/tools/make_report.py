# -*- coding: utf-8 -*-
import os
import re
import subprocess
from docx import Document
from docx.shared import Cm, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_LINE_SPACING, WD_TAB_ALIGNMENT, WD_TAB_LEADER
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

BASE = r"D:\java project"
ASSETS = os.path.join(BASE, "docs", "报告", "assets")
OUT = os.path.join(BASE, "docs", "报告", "Java语言及网络编程课程报告.docx")

INK = RGBColor(0x2B, 0x26, 0x20)
MUTED = RGBColor(0x7B, 0x72, 0x64)
ACCENT = RGBColor(0x8A, 0x5A, 0x2B)
WHITE = RGBColor(0xFF, 0xFF, 0xFF)

doc = Document()
first_chapter = True
headings = []


def set_run_font(run, name="宋体", size=12, bold=False, color=INK, italic=False):
    run.font.name = name
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.italic = italic
    run.font.color.rgb = color
    rPr = run._element.get_or_add_rPr()
    rFonts = rPr.find(qn("w:rFonts"))
    if rFonts is None:
        rFonts = OxmlElement("w:rFonts")
        rPr.append(rFonts)
    rFonts.set(qn("w:eastAsia"), name)


def style_font(style, name, size, bold=False, color=INK):
    style.font.name = name
    style.font.size = Pt(size)
    style.font.bold = bold
    style.font.color.rgb = color
    rPr = style.element.get_or_add_rPr()
    rFonts = rPr.find(qn("w:rFonts"))
    if rFonts is None:
        rFonts = OxmlElement("w:rFonts")
        rPr.append(rFonts)
    rFonts.set(qn("w:eastAsia"), name)


def setup_styles():
    normal = doc.styles["Normal"]
    style_font(normal, "宋体", 12)
    normal.paragraph_format.line_spacing = 1.5
    normal.paragraph_format.space_after = Pt(0)

    for name, size in [("Heading 1", 16), ("Heading 2", 14), ("Heading 3", 12)]:
        style = doc.styles[name]
        style_font(style, "黑体", size, bold=True)
        style.paragraph_format.space_before = Pt(12)
        style.paragraph_format.space_after = Pt(6)
        style.paragraph_format.keep_with_next = True


def setup_section(section, header_text=None, page_fmt=None, page_start=None):
    section.page_width = Cm(21.0)
    section.page_height = Cm(29.7)
    section.top_margin = Cm(2.54)
    section.bottom_margin = Cm(2.54)
    section.left_margin = Cm(3.0)
    section.right_margin = Cm(3.0)
    section.footer_distance = Cm(1.5)
    section.header_distance = Cm(1.5)

    section.header.is_linked_to_previous = False
    section.footer.is_linked_to_previous = False

    hp = section.header.paragraphs[0]
    hp.text = ""
    if header_text:
        hp.alignment = WD_ALIGN_PARAGRAPH.RIGHT
        run = hp.add_run(header_text)
        set_run_font(run, "宋体", 9, color=MUTED)

    fp = section.footer.paragraphs[0]
    fp.text = ""
    if page_fmt:
        fp.alignment = WD_ALIGN_PARAGRAPH.CENTER
        add_page_field(fp)

    if page_fmt:
        sectPr = section._sectPr
        pgNumType = sectPr.find(qn("w:pgNumType"))
        if pgNumType is None:
            pgNumType = OxmlElement("w:pgNumType")
            sectPr.append(pgNumType)
        pgNumType.set(qn("w:fmt"), page_fmt)
        if page_start is not None:
            pgNumType.set(qn("w:start"), str(page_start))


def add_page_field(paragraph):
    run = paragraph.add_run()
    set_run_font(run, "宋体", 9, color=MUTED)
    begin = OxmlElement("w:fldChar")
    begin.set(qn("w:fldCharType"), "begin")
    instr = OxmlElement("w:instrText")
    instr.set(qn("xml:space"), "preserve")
    instr.text = " PAGE "
    sep = OxmlElement("w:fldChar")
    sep.set(qn("w:fldCharType"), "separate")
    text = OxmlElement("w:t")
    text.text = "1"
    end = OxmlElement("w:fldChar")
    end.set(qn("w:fldCharType"), "end")
    for node in (begin, instr, sep, text, end):
        run._r.append(node)


def p(text, indent=True, size=12, bold=False, color=INK, align=WD_ALIGN_PARAGRAPH.JUSTIFY,
      space_after=6, line=1.5):
    para = doc.add_paragraph()
    para.alignment = align
    para.paragraph_format.line_spacing = line
    para.paragraph_format.space_after = Pt(space_after)
    if indent:
        para.paragraph_format.first_line_indent = Pt(size * 2)
    run = para.add_run(text)
    set_run_font(run, "宋体", size, bold=bold, color=color)
    return para


def bullets(items, size=12):
    for item in items:
        para = doc.add_paragraph(style="List Bullet")
        para.paragraph_format.line_spacing = 1.5
        para.paragraph_format.space_after = Pt(3)
        run = para.add_run(item)
        set_run_font(run, "宋体", size)


def h1(text):
    para = doc.add_heading(text, level=1)
    for run in para.runs:
        set_run_font(run, "黑体", 16, bold=True)
    headings.append((1, text))
    return para


def h2(text):
    para = doc.add_heading(text, level=2)
    for run in para.runs:
        set_run_font(run, "黑体", 14, bold=True)
    headings.append((2, text))
    return para


def h3(text):
    para = doc.add_heading(text, level=3)
    for run in para.runs:
        set_run_font(run, "黑体", 12, bold=True)
    headings.append((3, text))
    return para


def chapter(title):
    global first_chapter
    if not first_chapter:
        doc.add_page_break()
    h1(title)
    first_chapter = False


def caption(text):
    para = doc.add_paragraph()
    para.alignment = WD_ALIGN_PARAGRAPH.CENTER
    para.paragraph_format.space_before = Pt(3)
    para.paragraph_format.space_after = Pt(8)
    para.paragraph_format.keep_with_next = True
    run = para.add_run(text)
    set_run_font(run, "宋体", 10.5, color=MUTED)


def shade_cell(cell, fill):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:fill"), fill)
    tcPr.append(shd)


def remove_table_borders(table):
    tbl = table._tbl
    tblPr = tbl.tblPr
    for old in tblPr.findall(qn("w:tblBorders")):
        tblPr.remove(old)
    borders = OxmlElement("w:tblBorders")
    for edge in ("top", "left", "bottom", "right", "insideH", "insideV"):
        el = OxmlElement(f"w:{edge}")
        el.set(qn("w:val"), "nil")
        borders.append(el)
    layout = tblPr.find(qn("w:tblLayout"))
    if layout is not None:
        layout.addprevious(borders)
    else:
        look = tblPr.find(qn("w:tblLook"))
        if look is not None:
            look.addprevious(borders)
        else:
            tblPr.append(borders)


def prevent_row_split(table):
    for row in table.rows:
        trPr = row._tr.get_or_add_trPr()
        trPr.append(OxmlElement("w:cantSplit"))


def table(headers, rows, widths, caption_text=None, font_size=10.5):
    if caption_text:
        caption(caption_text)
    t = doc.add_table(rows=1, cols=len(headers))
    t.style = "Table Grid"
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    t.autofit = False
    header = t.rows[0].cells
    trPr = t.rows[0]._tr.get_or_add_trPr()
    trPr.append(OxmlElement("w:tblHeader"))
    for i, text in enumerate(headers):
        header[i].text = ""
        cell_p = header[i].paragraphs[0]
        cell_p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        run = cell_p.add_run(text)
        set_run_font(run, "宋体", font_size, bold=True)
        shade_cell(header[i], "F1E3CF")
        header[i].vertical_alignment = WD_ALIGN_VERTICAL.CENTER

    for row in rows:
        cells = t.add_row().cells
        for i, text in enumerate(row):
            cells[i].text = ""
            cell_p = cells[i].paragraphs[0]
            cell_p.alignment = WD_ALIGN_PARAGRAPH.LEFT
            cell_p.paragraph_format.space_after = Pt(0)
            cell_p.paragraph_format.line_spacing = 1.15
            run = cell_p.add_run(str(text))
            set_run_font(run, "宋体", font_size)
            cells[i].vertical_alignment = WD_ALIGN_VERTICAL.CENTER

    for i, width in enumerate(widths):
        for row in t.rows:
            row.cells[i].width = Cm(width)
    prevent_row_split(t)
    doc.add_paragraph().paragraph_format.space_after = Pt(0)


def figure(filename, width_cm, caption_text):
    path = os.path.join(ASSETS, filename)
    doc.add_picture(path, width=Cm(width_cm))
    last = doc.paragraphs[-1]
    last.alignment = WD_ALIGN_PARAGRAPH.CENTER
    caption(caption_text)


def code(text):
    for line in text.splitlines():
        para = doc.add_paragraph()
        para.paragraph_format.left_indent = Cm(0.6)
        para.paragraph_format.line_spacing = 1.0
        para.paragraph_format.space_after = Pt(0)
        para.paragraph_format.space_before = Pt(0)
        run = para.add_run(line if line else " ")
        set_run_font(run, "Consolas", 9)
        pPr = para._p.get_or_add_pPr()
        shd = OxmlElement("w:shd")
        shd.set(qn("w:fill"), "F6F2E9")
        pPr.append(shd)
    doc.add_paragraph().paragraph_format.space_after = Pt(0)


def toc_field():
    para = doc.add_paragraph()
    run = para.add_run()
    begin = OxmlElement("w:fldChar")
    begin.set(qn("w:fldCharType"), "begin")
    begin.set(qn("w:dirty"), "true")
    instr = OxmlElement("w:instrText")
    instr.set(qn("xml:space"), "preserve")
    instr.text = ' TOC \\o "1-3" \\h \\z \\u '
    sep = OxmlElement("w:fldChar")
    sep.set(qn("w:fldCharType"), "separate")
    text = OxmlElement("w:t")
    text.text = "打开文档后按 Ctrl+A、F9 更新目录"
    end = OxmlElement("w:fldChar")
    end.set(qn("w:fldCharType"), "end")
    for node in (begin, instr, sep, text, end):
        run._r.append(node)


def cover():
    section = doc.sections[0]
    setup_section(section, header_text=None, page_fmt=None)

    for _ in range(1):
        doc.add_paragraph()

    para = doc.add_paragraph()
    para.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = para.add_run("中国矿业大学计算机学院")
    set_run_font(run, "黑体", 22, bold=True)

    para = doc.add_paragraph()
    para.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = para.add_run("20××级本科生课程报告")
    set_run_font(run, "宋体", 18)

    doc.add_paragraph()

    para = doc.add_paragraph()
    para.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = para.add_run("五子棋在线对战平台的设计与实现")
    set_run_font(run, "黑体", 26, bold=True)

    para = doc.add_paragraph()
    para.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = para.add_run("——基于 Spring Boot 3 与 WebSocket 的实时对战系统")
    set_run_font(run, "宋体", 16, color=MUTED)

    for _ in range(2):
        doc.add_paragraph()

    info_rows = [
        ("课程名称", "Java语言及网络编程"),
        ("学生姓名", "________________"),
        ("学　　号", "________________"),
        ("专　　业", "________________"),
        ("任课教师", "________________"),
    ]
    for label, value in info_rows:
        para = doc.add_paragraph()
        para.alignment = WD_ALIGN_PARAGRAPH.LEFT
        para.paragraph_format.left_indent = Cm(5.0)
        para.paragraph_format.line_spacing = 1.5
        run = para.add_run(label + "：")
        set_run_font(run, "宋体", 16)
        run = para.add_run(value)
        set_run_font(run, "宋体", 16)

    doc.add_paragraph()

    para = doc.add_paragraph()
    para.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = para.add_run("2026 年 11 月")
    set_run_font(run, "宋体", 16)


def toc_section():
    section = doc.add_section(WD_SECTION.NEW_PAGE)
    setup_section(section, header_text=None, page_fmt="lowerRoman", page_start=1)

    para = doc.add_paragraph()
    para.alignment = WD_ALIGN_PARAGRAPH.CENTER
    para.paragraph_format.space_before = Pt(12)
    para.paragraph_format.space_after = Pt(18)
    run = para.add_run("目  录")
    set_run_font(run, "黑体", 18, bold=True)

    toc_field()


def body_section():
    section = doc.add_section(WD_SECTION.NEW_PAGE)
    setup_section(section, header_text="《Java语言及网络编程》课程报告",
                  page_fmt="decimal", page_start=1)


def main():
    setup_styles()
    cover()
    toc_section()
    body_section()

    chapter("摘要")
    p("本报告完成了一个基于 Java 17、Spring Boot 3.5.7、MySQL 8.0 与 Spring Data JPA 的五子棋在线对战系统。"
      "系统采用 B/S 架构，页面由 Thymeleaf 服务端渲染，实时对局通过原生 WebSocket 承载，服务端作为唯一权威完成落子校验、"
      "胜负判定、计时与结算。系统实现了注册登录、按评分匹配、对局聊天、断线重连、ELO 天梯与排行榜等功能，"
      "并通过会话、CSRF、BCrypt、登录锁定、限流与安全响应头等措施保证基本安全。测试方面，共设计并执行 58 个自动化用例，"
      "覆盖棋盘规则、评分算法、认证安全、WebSocket 双客户端完整对局与聊天，全部用例通过。项目还完成了可执行 jar、"
      "Docker Compose 部署文件与 k6 压测脚本。结果表明，该方案能够在单机环境下稳定完成完整对战闭环，"
      "并为后续扩展观战、禁手与多实例部署留下接口。", indent=True)
    p("关键词：五子棋；Spring Boot；WebSocket；MySQL；ELO；服务端权威", indent=False, bold=True)

    chapter("第 1 章 绪论")
    h2("1.1 选题背景与意义")
    p("网络编程课程通常从 Socket、TCP/UDP 与 HTTP 讲起，最终要求学生完成一个具备真实交互的完整应用。"
      "五子棋规则简洁、状态空间可控，但一个可用的在线对战系统需要同时解决身份认证、实时通信、并发状态同步、"
      "公平裁决、断线恢复与排名计算等问题，能够把课程中的网络协议、数据库、软件工程与安全知识串联起来。")
    p("与单机版五子棋相比，在线版的核心难点不在界面，而在“谁说了算”。如果由客户端判断胜负，"
      "修改前端脚本即可伪造结果；如果采用真正的点对点连接，服务端又无法验证棋局。因此，本项目将“服务端权威”"
      "作为设计主线，所有落子、轮次、胜负与超时都由服务端裁决，客户端只负责渲染与交互。")

    h2("1.2 课程要求与项目定位")
    p("课程报告说明要求完成需求分析、方案论证、系统验证与汇报四个阶段，并给出了传统软件工程与 AI 大模型工程化"
      "两条实现路线。本项目采用“传统软件工程为主线、AI 工程化辅助”的方式：需求、设计、数据库与协议由人工确定，"
      "编码阶段借助 AI 生成与重构，并用可执行的测试集与人工审批门禁控制风险。")
    p("在课程材料提交方面，报告需要包含封面、自动目录、分页正文与开发环境说明；项目源码、数据库脚本、"
      "测试记录与部署文档需要一并打包。本报告与仓库中的验证报告、AI 记录共同构成完整的实验材料。")

    h2("1.3 主要工作与成果")
    bullets([
        "完成 Spring Boot 3 + Java 17 + MySQL 8 + JPA 的工程骨架、Flyway 迁移与会话认证。",
        "实现 15×15 棋盘、四方向五连判定、落子合法性校验、30 秒计时与胜负/平局/中止结算。",
        "实现基于 WebSocket 的匹配、对局、聊天、断线与重连，消息在事务提交后广播。",
        "实现 ELO 评分、评分流水、Top 50 排行榜与“我的排名”。",
        "实现 Thymeleaf + 原生 JS + Canvas 的登录、注册、大厅、对局与榜单页面。",
        "完成 58 个自动化用例、可执行 jar、Docker Compose、k6 脚本与 6 份阶段验证报告。",
    ])
    p("代码规模约 2850 行 Java 主代码、1366 行测试代码、264 行模板与 1252 行 CSS/JS；本地共 11 个提交，"
      "远程仓库为 GitHub 的 gomoku-online。")

    h2("1.4 报告结构")
    p("第 2 章给出功能、非功能与安全需求；第 3 章论证技术选型与 P2P 取舍；第 4 章给出架构、数据库、协议与状态机设计；"
      "第 5 章说明关键实现；第 6 章给出测试、验证、性能分析与缺陷记录；第 7 章总结并展望。附录给出协议、部署与 AI 工程化摘要。")

    chapter("第 2 章 需求分析")
    h2("2.1 功能需求")
    p("系统面向普通玩家与服务端两类角色。玩家的核心用例包括注册登录、开始匹配、进行对局、发送聊天、查看排行榜与断线重连；"
      "服务端负责认证、匹配、裁决、计时、结算与持久化。")
    table(
        ["编号", "需求", "说明", "验收要点"],
        [
            ["FR-01", "注册与登录", "用户名唯一、密码 BCrypt 存储；登录失败锁定", "重复用户名拒绝；弱密码拒绝；5 次失败锁定"],
            ["FR-02", "匹配", "按评分寻找对手，等待越久分段越宽", "双方入队后 5 秒内匹配；对局中不可重复入队"],
            ["FR-03", "对局", "15×15、黑先、五连判胜、满盘判和、认输、30 秒/步", "服务端校验轮次、坐标、空位、序号"],
            ["FR-04", "聊天", "棋盘下方聊天栏，消息实时送达并落库", "200 字上限、1 条/秒、双方可见"],
            ["FR-05", "天梯", "ELO 评分、排行榜、我的排名", "胜负后评分变化；Top 50；同分并列"],
            ["FR-06", "断线", "掉线通知、60 秒宽限、重连恢复、超时判负", "重连后棋盘/轮次/聊天恢复"],
        ],
        [1.6, 2.6, 6.2, 6.8],
        "表 2-1 功能需求与验收要点",
    )

    doc.add_page_break()
    h2("2.2 非功能需求")
    table(
        ["编号", "类别", "目标", "说明"],
        [
            ["NFR-01", "性能", "单实例 200 个 WebSocket 连接；落子端到端 p95 < 100 ms；榜单 p95 < 30 ms", "局域网环境；单机、无反向代理"],
            ["NFR-02", "可用性", "断线 60 秒内可恢复；服务端超时自动结算", "避免一局因单个客户端掉线永久卡住"],
            ["NFR-03", "可维护性", "分层清晰、规则可单测、schema 可迁移", "Flyway 管理表结构；服务端逻辑与页面解耦"],
            ["NFR-04", "兼容性", "Chrome / Edge 等现代浏览器；窄屏可用", "Canvas + 原生 ES Module，无前端构建链"],
        ],
        [1.6, 1.8, 8.2, 5.6],
        "表 2-2 非功能需求",
    )

    h2("2.3 安全需求")
    p("安全需求按常见 Web 风险梳理：密码不能明文或弱哈希存储；会话必须不可被脚本读取；写操作要防 CSRF；"
      "聊天与昵称要防 XSS；数据库访问要防注入；接口要防越权与暴力尝试；WebSocket 握手也要认证。")
    table(
        ["风险", "措施", "验证方式"],
        [
            ["密码泄露", "BCrypt strength 12；密码 8–72 字节且含字母数字", "查库前缀；单测/集成测试"],
            ["会话劫持", "HttpOnly + SameSite=Lax；30 分钟超时；登录后 session fixation 防护", "响应头与安全测试"],
            ["CSRF", "表单自动附带 token；fetch 读取 meta token", "无 token 提交被拒绝"],
            ["XSS", "Thymeleaf 转义；聊天用 textContent；CSP script-src 'self'", "XSS 载荷按文本显示"],
            ["SQL 注入", "JPA 参数绑定，不拼接 SQL", "代码审查与输入校验"],
            ["暴力登录", "用户名+IP 5 次失败锁 15 分钟；注册限流", "集成测试与端到端"],
            ["越权", "对局页/WS 操作校验 userId 属于 black/white", "非参与者访问被拒绝"],
            ["WebSocket 未授权", "会话 Cookie + 5 分钟 ticket；握手拦截器校验", "无 ticket/无会话连接被关闭"],
        ],
        [3.2, 8.8, 5.2],
        "表 2-3 安全需求与措施",
    )

    h2("2.4 约束与边界")
    p("课程周期内明确不做以下内容：真 P2P/WebRTC、观战、好友房、悔棋、禁手规则、头像上传、邮箱验证与多实例集群。"
      "系统按单实例部署，房间、匹配队列、登录锁定、限流与 ticket 均保存在内存；应用重启会丢失进行中的对局，"
      "但数据库保留已完成对局与评分流水。上述边界在第 3 章方案论证与第 7 章展望中进一步说明。")

    h2("2.5 需求验收标准")
    p("为了让需求“可验收”，项目把每条需求映射到测试类或用例编号：AUTH 对应认证与安全测试，GAME 对应棋盘与对局测试，"
      "WS 对应双客户端集成测试，CHAT 对应聊天测试，RATING 对应 ELO 与榜单测试。该映射同时作为 AI 工程化的规格输入，"
      "详见附录 C 与仓库中的 skills/gomoku-spec/SKILL.md。")

    chapter("第 3 章 方案论证")
    h2("3.1 技术选型")
    table(
        ["层次", "选型", "理由"],
        [
            ["语言/运行时", "Java 17（Temurin）", "课程要求；LTS；record、switch 表达式等特性便于表达 DTO 与规则"],
            ["框架", "Spring Boot 3.5.7", "自动配置、内嵌 Tomcat、与 Security/JPA/WebSocket 集成成熟"],
            ["安全", "Spring Security 6", "会话、CSRF、密码编码器、过滤器链均为标准实现"],
            ["持久化", "Spring Data JPA + Hibernate 6", "实体映射、事务、乐观锁；减少手写 SQL"],
            ["数据库", "MySQL 8.0 + Flyway", "课程要求；窗口函数/索引；Flyway 保证 schema 可重复"],
            ["页面", "Thymeleaf + 原生 JS", "服务端渲染、同源部署、无 Node 构建链；贴合课程主题"],
            ["实时", "Spring WebSocket（原生 Handler）", "自定义 JSON 协议，便于在报告中说明帧与状态机"],
            ["测试", "JUnit 5 + MockMvc + StandardWebSocketClient", "覆盖单元、MVC 与真实双客户端对局"],
        ],
        [3.0, 6.0, 8.0],
        "表 3-1 技术选型",
    )

    h2("3.2 实时通信方案对比")
    p("需求最初提出“用户直接通过 P2P 连接”。分析后发现，浏览器端的真 P2P 只能使用 WebRTC DataChannel，"
      "它仍然需要信令服务器交换 SDP/ICE，失败时还要 TURN 中继；校园网对 UDP 的限制会使穿透成功率不可控。"
      "更关键的是，真 P2P 下服务端看不到棋局，胜负由客户端上报，天梯结果可以被伪造。五子棋是回合制棋类，"
      "P2P 的低延迟收益很小，却引入 NAT、作弊与重连三类问题。")
    table(
        ["方案", "实现", "优点", "缺点", "结论"],
        [
            ["服务端 WebSocket 中继", "Spring WebSocket + 自定义协议", "跨网稳定、可裁决、可重连、可回放", "非物理直连", "采用"],
            ["WebRTC DataChannel", "信令 + STUN/TURN", "真 P2P、低延迟", "穿透不稳、需 TURN、排位不可信、重连难", "不采用"],
            ["裸 TCP/UDP 客户端", "Netty + ICE/UPnP", "最贴近底层网络编程", "工作量大、演示不便、仍要穿透", "不采用"],
        ],
        [3.2, 4.0, 4.4, 5.0, 1.4],
        "表 3-2 实时通信方案对比",
    )
    p("最终把“P2P”重新定义为“对局双方会话直连、动作仅在两名玩家之间同步”，由服务端做权威中继。"
      "这样既保留了双方实时对战的体验，又保住了天梯的可信度。该取舍也写入了项目文档，作为方案论证的一部分。")

    h2("3.3 服务端权威与一致性")
    p("服务端权威体现在四点：轮次由服务端维护；落点合法性由服务端校验；胜负由服务端判定；超时由服务端计时。"
      "客户端不做乐观落子，只有收到 MOVE 广播后才更新棋盘，避免两端状态分叉。")
    p("每一步落子写入 moves 表，并以 (match_id, seq) 唯一约束保证幂等与顺序；断线重连时服务端按 seq 重放 moves，"
      "恢复棋盘与轮次。广播统一在事务提交后执行，避免客户端先看到“已结算”而数据库尚未提交的窗口。")

    h2("3.4 匹配与评分方案")
    p("匹配采用内存等待队列：按入队时间排序，评分差从 100 开始，每等待 5 秒放宽 50，上限 400。"
      "配对成功后创建 WAITING 对局，双方进入对局页并 RESUME，服务端在两人都连接后置为 PLAYING。")
    p("评分采用 ELO：E = 1 / (1 + 10^((Rb-Ra)/400))，Δ = K × (S − E)。新账号 K=32，达到 30 局后 K=16，"
      "评分下限 100；平局按 0.5 计；双方掉线中止（ABORT）不计分。排行榜按评分降序、同分按 id 升序，"
      "名次用“严格大于我的人数 + 1”计算，天然支持并列。")

    h2("3.5 认证与会话方案")
    p("页面与接口同源部署，因此选择会话 Cookie（HttpOnly、SameSite=Lax）而不是把 JWT 放在 localStorage。"
      "会话方案对 XSS 更不敏感，也能让 WebSocket 握手直接复用登录态。为兼容不转发 Cookie 的 WebSocket 客户端，"
      "系统额外签发 5 分钟、绑定 userId 的 ticket，连接地址为 /ws/game?ticket=...；没有 ticket 时回退到会话认证。")

    h2("3.6 方案结论")
    p("综合来看，Spring Boot 单体 + 服务端权威 WebSocket + MySQL 持久化是课程周期内风险最低、可验证性最强的方案。"
      "它避免了真 P2P 的穿透与公平性问题，同时保留了实时对战、聊天、重连与排名等完整能力，便于在第 6 章逐项验证。")

    chapter("第 4 章 系统设计")
    h2("4.1 总体架构")
    p("系统为单进程 B/S 架构：浏览器加载 Thymeleaf 页面，页面中的原生 JS 建立一条 WebSocket 连接，"
      "同时承载匹配、对局与聊天；Spring Boot 内部按职责拆分为安全、WebSocket 网关、对局引擎、匹配、评分与聊天模块；"
      "MySQL 保存用户、对局、落子、聊天与评分流水。")
    figure("architecture.png", 15.5, "图 4-1 系统总体架构")

    h2("4.2 模块划分")
    table(
        ["模块", "职责", "关键类"],
        [
            ["security", "会话、CSRF、登录锁定、限流、审计、安全头", "SecurityConfig, LoginAttemptService, RateLimiter"],
            ["user", "注册、登录、用户信息", "User, UserService, AuthController"],
            ["game", "规则、房间、落子、计时、结算", "Board, GameRoom, GameService, GameTimeoutScheduler"],
            ["match", "等待队列、定时配对", "MatchmakingService, MatchmakingScheduler"],
            ["rating", "ELO、评分流水、榜单", "EloRatingService, LeaderboardService"],
            ["chat", "消息校验、落库、历史", "ChatService, ChatRepository"],
            ["ws", "握手鉴权、协议编解码、会话注册", "GameWebSocketHandler, WsSessionRegistry, WsTicketService"],
        ],
        [2.2, 7.4, 7.4],
        "表 4-1 模块划分",
    )

    h2("4.3 数据库设计")
    p("数据库共 5 张业务表。users 保存账号、评分与胜负场次；game_matches 保存对局元信息；moves 保存每一步落子，"
      "是重连与回放的依据；chat_messages 保存聊天；rating_history 保存评分变化。所有表使用 InnoDB 与 utf8mb4。")
    figure("er.png", 15.5, "图 4-2 数据库 ER 关系")
    table(
        ["表", "关键字段", "索引/约束"],
        [
            ["users", "id, username, password_hash, nickname, rating, wins/losses/draws, version", "username 唯一；rating 索引；version 乐观锁"],
            ["game_matches", "id, black_id, white_id, status, result, winner_id, started_at, ended_at", "black_id/white_id 索引；外键"],
            ["moves", "id, match_id, seq, player_id, x, y", "(match_id, seq) 唯一；match_id 外键级联"],
            ["chat_messages", "id, match_id, sender_id, content, created_at", "(match_id, id) 索引；级联删除"],
            ["rating_history", "id, user_id, match_id, before/after/delta, created_at", "user_id 索引；外键"],
        ],
        [2.8, 8.8, 5.4],
        "表 4-2 数据表说明",
    )

    h2("4.4 WebSocket 协议设计")
    p("协议采用统一信封：op 表示操作，seq 表示落子序号，data 为业务字段。客户端到服务端的操作包括 QUEUE_JOIN、"
      "QUEUE_LEAVE、RESUME、MOVE、CHAT、RESIGN 与 PING；服务端到客户端包括 QUEUED、MATCH_FOUND、STATE、MOVE、"
      "CHAT、GAME_OVER、OPPONENT_LEFT、OPPONENT_BACK、ERROR 与 PONG。")
    code('{\n  "op": "MOVE",\n  "seq": 3,\n  "data": { "matchId": 12, "x": 7, "y": 8 }\n}')
    table(
        ["方向", "op", "data", "说明"],
        [
            ["C→S", "QUEUE_JOIN / QUEUE_LEAVE", "—", "入队/退队"],
            ["C→S", "RESUME", "{matchId}", "进入或恢复对局，返回 STATE"],
            ["C→S", "MOVE", "{matchId,x,y,seq}", "落子"],
            ["C→S", "CHAT", "{matchId,text}", "聊天"],
            ["C→S", "RESIGN", "{matchId}", "认输"],
            ["S→C", "MATCH_FOUND", "{matchId,color,opponent}", "匹配成功"],
            ["S→C", "STATE", "{moves,chats,turn,seq,deadline,...}", "全量状态（重连）"],
            ["S→C", "MOVE", "{seq,x,y,playerId,nextTurn,deadline}", "落子广播"],
            ["S→C", "GAME_OVER", "{result,winnerId,deltas,ratings,winningLine}", "结算"],
            ["S→C", "ERROR", "{code,message}", "错误"],
        ],
        [1.2, 4.2, 6.6, 3.0],
        "表 4-3 WebSocket 协议",
        font_size=9.5,
    )
    p("错误码包括 NOT_YOUR_TURN、INVALID_MOVE、SEQ_MISMATCH、GAME_OVER、NOT_IN_GAME、RATE_LIMITED 等。"
      "单条消息限制 4 KB；同一用户只保留一条活跃连接，新连接会以 4001 关闭旧连接。")

    h2("4.5 对局状态机")
    p("对局从 WAITING 开始，双方 RESUME 且都连接后进入 PLAYING；五连、认输、超时或满盘后进入 FINISHED；"
      "双方都在宽限期内未重连时进入 ABORTED（ABORT），不计胜负与评分。连接状态独立为 CONNECTED / OFFLINE，"
      "OFFLINE 超过 60 秒触发判负，60 秒内 RESUME 可恢复。")

    h2("4.6 关键流程")
    p("匹配与对局的时序如下：双方入队，服务端配对并下发 MATCH_FOUND；双方进入对局页后 RESUME，服务端回 STATE；"
      "之后每次 MOVE 由服务端校验并广播；聊天在房间内广播并落库；结束时服务端计算 ELO 并下发 GAME_OVER。")
    figure("sequence.png", 15.5, "图 4-3 匹配与对局时序")

    h2("4.7 安全设计")
    p("安全设计贯穿认证、传输、输入、授权与审计五个层面：认证使用 BCrypt 与会话；传输层在部署时启用 HTTPS/WSS；"
      "输入使用 Jakarta Validation 与聊天长度限制；授权在对局页与 WS 操作中校验参与者；审计记录登录成功/失败、"
      "锁定与注册。CSP、X-Frame-Options、Referrer-Policy 与 Permissions-Policy 由 Spring Security 统一注入。")

    chapter("第 5 章 系统实现")
    h2("5.1 工程结构")
    code("""src/main/java/com/example/gomoku/
├── chat/      聊天服务与实体
├── common/    异常与错误码
├── config/    Security / WebSocket 配置
├── game/      棋盘、房间、对局服务、计时
├── match/     匹配队列与调度
├── rating/    ELO、评分流水、榜单
├── security/  登录锁定、限流、审计
├── user/      用户、注册/登录、大厅
└── ws/        WebSocket 处理器、会话注册、ticket""")

    h2("5.2 认证与安全实现")
    p("密码使用 BCrypt strength 12 编码；登录失败按“用户名 + IP”计数，5 次后锁定 15 分钟；成功登录清零。"
      "登录锁定过滤器位于 UsernamePasswordAuthenticationFilter 之前，即使密码正确也会在锁定期内被拦截。"
      "注册接口按 IP 限制 10 分钟 5 次，超出提示“操作过于频繁”。")
    p("会话 Cookie 设置 HttpOnly 与 SameSite=Lax，30 分钟超时；CSRF 由 Spring Security 默认开启，"
      "Thymeleaf 表单自动附带 token，fetch 从 meta 读取。安全响应头包括 CSP、X-Frame-Options=DENY、"
      "Referrer-Policy=no-referrer 与 Permissions-Policy。")

    h2("5.3 棋局引擎实现")
    p("棋盘使用 15×15 的 Stone 二维数组，落子前校验坐标、空位与轮次。胜负判定只围绕最后一手向四个方向统计连续同色棋子，"
      "复杂度与棋盘大小无关；同时返回完整连线坐标，供前端高亮。")
    code("""for (int[] dir : new int[][]{{1,0},{0,1},{1,1},{1,-1}}) {
    int count = 1;
    count += countSide(x, y, dir[0], dir[1], stone);
    count += countSide(x, y, -dir[0], -dir[1], stone);
    if (count >= 5) return true;
}""")
    p("对局服务持有内存房间，房间锁保证同一局的落子串行；每步写入 moves 表，seq 递增；断线重连时按 moves 重放。"
      "计时器每秒扫描房间，当前回合超时则判对手胜；一方掉线超过宽限期同样判负，双方都掉线则中止。")
    figure("board.png", 10.5, "图 5-1 棋盘渲染效果（Canvas 绘制，含最后一手与胜利连线）")

    h2("5.4 WebSocket 与对局服务")
    p("WebSocket 端点 /ws/game 由 Spring 原生 Handler 处理。握手阶段先由 HttpSessionHandshakeInterceptor 复制会话属性，"
      "再由 WsTicketInterceptor 校验 ticket；处理器从属性或 SecurityContext 中取出 userId，注册会话。"
      "同一用户重复连接时，新会话替换旧会话，旧连接以 4001 关闭。")
    p("所有广播都在事务提交后执行，使用 TransactionSynchronizationManager 注册 afterCommit 回调。"
      "这样客户端收到 GAME_OVER 时，数据库中的对局状态与评分已经落盘，测试中“先收消息再查库”也能保持一致。")

    h2("5.5 匹配实现")
    p("匹配队列保存在内存中，按入队时间排序；每次配对从最早等待的玩家开始，寻找评分差在允许范围内的对手。"
      "允许差值 = min(400, 100 + 50 × floor(等待秒数 / 5))。配对成功后随机决定黑白，创建 WAITING 对局并下发 MATCH_FOUND。")

    h2("5.6 聊天与断线处理")
    p("聊天消息先 strip 空白，校验非空与 200 字上限，再按 userId 做 1 条/秒限流；通过后落库并广播给房间内两人。"
      "聊天历史只保留最近 50 条，随 STATE 下发，重连后可以恢复上下文。前端使用 textContent 渲染，避免 XSS。")
    p("断线时服务端标记 offline 并通知对手；60 秒内 RESUME 会清除标记、回 STATE 并通知对手“已重连”。"
      "超过宽限期则判离线方负；双方都超时则 ABORT，不写评分流水。")

    h2("5.7 ELO 与排行榜")
    p("评分计算独立为 RatingService 接口，EloRatingService 实现标准 ELO。结算时在同一事务内更新双方评分、胜负场次，"
      "并写入两条 rating_history。GAME_OVER 携带 deltas 与 ratings，前端在结果弹层显示“本局 +16 分 · 当前 1216 分”。")
    code("""double expected = 1.0 / (1.0 + Math.pow(10.0, (whiteRating - blackRating) / 400.0));
int deltaBlack = (int) Math.round(K * (scoreBlack - expected));
int newBlack = Math.max(100, blackRating + deltaBlack);""")
    p("排行榜使用 rating 降序、id 升序分页查询；同一评分共享名次。页面展示 Top 50、当前用户高亮与“我的排名”。")

    h2("5.8 前端实现")
    p("前端不引入框架与构建链，使用原生 ES Module 拆分 ws.js、board.js、game.js、lobby.js。棋盘用 Canvas 绘制木纹渐变、"
      "网格、星位、棋子、最后一手与胜利连线；鼠标悬停时显示半透明预落子。")
    p("联调中发现鼠标落点与棋子位置不重合：画布逻辑尺寸被限制在 640px，而 CSS 显示宽度更大，导致点击坐标被拉伸。"
      "修复方式是直接使用 getBoundingClientRect 的实时宽高计算网格，并用 ResizeObserver 在布局变化时重绘；"
      "同时给画布加 max-width: 640px 并居中。修复后落点与棋子完全一致。")
    figure("login_crop.png", 9.0, "图 5-2 登录页面")
    figure("register_crop.png", 9.0, "图 5-3 注册页面")

    h2("5.9 部署实现")
    p("项目提供三种运行方式：开发环境 scripts/dev.bat（自动选择 JDK 17）、可执行 jar、以及 Docker Compose。"
      "Dockerfile 基于 eclipse-temurin:17-jre，使用非 root 用户；compose 启动 MySQL 8.0 与 app，MySQL 映射宿主机 3307，"
      "避免与本机 3306 冲突。生产 profile 开启模板/静态资源缓存并降低日志级别，HTTPS 部署时通过环境变量开启 Secure Cookie。")

    chapter("第 6 章 系统验证")
    h2("6.1 测试策略")
    p("验证按“由内到外”分为六层：L0 单元测试、L1 集成测试、L2 WebSocket 双客户端测试、L3 浏览器双会话、"
      "L4 局域网真机、L5 公网隧道、L6 压测。课程提交以 L0–L2 自动化为主，L3/L4 手工验证并留存截图。")
    table(
        ["层级", "验证内容", "工具/方式"],
        [
            ["L0", "棋盘规则、ELO、密码策略、限流/锁定", "JUnit 5"],
            ["L1", "注册/登录/榜单/安全头/API 401", "SpringBootTest + MockMvc + MySQL"],
            ["L2", "匹配→对局→聊天→结算全链路", "同 JVM 两个 StandardWebSocketClient"],
            ["L3", "双浏览器真实 UI、断线重连", "Chrome + Edge"],
            ["L4", "手机/另一台电脑", "局域网 10.4.250.84"],
            ["L5", "外网演示", "ngrok / cloudflared 隧道"],
            ["L6", "并发与延迟", "k6 WebSocket 脚本"],
        ],
        [1.4, 8.6, 6.0],
        "表 6-1 测试分层",
    )

    h2("6.2 自动化测试结果")
    p("执行 mvn test，共 58 个用例全部通过。测试覆盖棋盘、评分、认证安全、页面渲染、聊天、断线判负与真实双客户端对局。")
    table(
        ["测试类", "数量", "覆盖"],
        [
            ["BoardTest", "13", "四方向五连、长连、边界、满盘、连线坐标"],
            ["GameServiceTest", "8", "五连结算、非法落子、认输、掉线判负、宽限内重连"],
            ["GameWebSocketIntegrationTest", "1", "真实端口 + 两个 WS 客户端：匹配、9 步、聊天、结算、数据库校验"],
            ["SecurityIntegrationTest", "5", "锁定、成功清零、安全头、API 401、注册限流"],
            ["LoginAttemptServiceTest / RateLimiterTest", "8", "锁定窗口、限流窗口、key 隔离"],
            ["ChatServiceTest", "4", "空消息、超长、限流、去空白"],
            ["EloRatingServiceTest / LeaderboardServiceTest", "7", "同分、K 值、下限、ABORT、排序与并列"],
            ["PageRenderTest / 其他", "12", "登录/注册/大厅渲染、BCrypt、表单校验、注册逻辑"],
        ],
        [5.6, 1.4, 9.0],
        "表 6-2 自动化测试分布",
    )

    h2("6.3 端到端验证")
    p("启动应用后，用两个浏览器分别注册账号并同时匹配，验证了：匹配成功自动跳转、黑棋先手、落子同步、聊天实时到达、"
      "五连结算、认输、断线 60 秒内重连与超时判负。真实 WebSocket 集成测试在随机端口启动完整应用，"
      "以两个客户端完成 9 步对局并断言数据库中的对局状态、落子数量、胜负与评分变化。")
    p("页面检查包括：登录/注册页渲染、大厅 ticket 下发、对局页 Canvas 与聊天输入、排行榜排序与“我的排名”。"
      "图 6-1 为对局页棋盘渲染效果，图 6-2 为登录页。")

    h2("6.4 安全验证")
    table(
        ["用例", "期望", "结果"],
        [
            ["弱密码注册", "拒绝并提示规则", "通过"],
            ["重复用户名", "提示已被占用", "通过"],
            ["连续 5 次错误登录", "账号锁定；正确密码也被拒绝", "通过"],
            ["未登录访问 /api/me", "401", "通过"],
            ["无 CSRF token 提交", "被拒绝", "通过"],
            ["非参与者访问对局", "403/错误", "通过"],
            ["无 ticket/无会话 WebSocket", "4401 关闭", "通过"],
            ["聊天 XSS 载荷", "按文本显示", "通过"],
        ],
        [6.4, 6.6, 3.0],
        "表 6-3 安全测试",
    )

    h2("6.5 性能分析")
    p("从复杂度看，落子校验为 O(1)，胜负判定只扫描最后一手四个方向（最多 4×15 格），排行榜依赖 users.rating 索引，"
      "聊天与落子查询均按 match_id 建索引；连接池上限 10，WebSocket 每用户一条连接。单机瓶颈主要在网络与数据库写入，"
      "而不是规则计算。")
    p("本机 HTTP 冒烟（Python http.client，20 并发、200 次 GET /login，每次新建连接）结果：p50=36.8 ms、p95=65.2 ms、"
      "p99=85.0 ms、平均 38.9 ms，200 次全部返回 200。该数据包含 Python 客户端与 TCP 建连开销，仅作为本机基线，"
      "不等同于正式压测。WebSocket 200 并发与 k6 数据尚未采集，脚本已提供，答辩前可在目标环境执行。")

    h2("6.6 需求达成情况")
    table(
        ["需求", "达成情况", "证据"],
        [
            ["注册登录与安全", "达成", "BCrypt、锁定、限流、CSRF、安全头、安全测试"],
            ["对局聊天", "达成", "聊天落库、1 条/秒、最近 50 条、双端广播"],
            ["天梯排位", "达成", "ELO、rating_history、Top 50、我的排名"],
            ["匹配与查看排名", "达成", "队列 + 分段放宽；/leaderboard 与 /api/leaderboard"],
            ["界面简洁美观", "达成", "Thymeleaf + Canvas，响应式，暖木色主题"],
            ["P2P 直连", "按论证调整", "改为服务端权威中继，报告中给出 P2P 对比与取舍"],
        ],
        [4.4, 3.2, 8.4],
        "表 6-4 需求达成情况",
    )

    h2("6.7 缺陷与修复记录")
    table(
        ["问题", "根因", "修复"],
        [
            ["Hibernate schema 校验失败", "moves.x/y 为 TINYINT，实体为 int", "新增 V2 迁移改为 INT"],
            ["WebSocket 测试 200 无法升级", "测试客户端不转发 Cookie 头", "增加短时 ticket 握手认证"],
            ["鼠标落点偏移", "画布绘制尺寸被限制 640，显示尺寸更大", "按实际 rect 计算并由 ResizeObserver 重绘"],
            ["测试清理外键失败", "rating_history 引用 match", "清理顺序改为先流水、再对局、最后用户"],
            ["dev.bat 中文乱码", "UTF-8 + LF + 中文批处理", "改为 ASCII + CRLF，并优先 JDK 17"],
        ],
        [5.0, 6.6, 5.4],
        "表 6-5 主要缺陷与修复",
    )

    h2("6.8 本章小结")
    p("从单元规则到真实双客户端对局，系统的主要路径均有自动化断言；安全项与页面项也做了针对性验证。"
      "性能方面完成了复杂度分析与本机 HTTP 基线，WebSocket 压测留待目标环境执行。整体达成课程要求，"
      "并保留了可复现的测试与验证报告。")

    chapter("第 7 章 总结与展望")
    h2("7.1 总结")
    p("本项目完成了一个可运行、可测试、可部署的五子棋在线对战系统。核心收获有三点：第一，实时对战的关键不是 WebSocket API，"
      "而是服务端权威与状态一致性；第二，安全需要贯穿认证、输入、授权与审计，而不是只在登录页加一个密码框；"
      "第三，AI 辅助编码必须配合可执行规格与测试，否则“看起来能跑”的代码很难在真实对局中稳定。")
    p("项目最终形成 6 个阶段、58 个自动化用例、11 次提交、1 份可执行 jar 与 1 套 Docker 部署文件；"
      "每层都有验证报告，便于课程答辩时逐项说明。")

    h2("7.2 不足")
    bullets([
        "单实例内存状态：房间、匹配队列、锁定、限流与 ticket 都依赖单进程，重启会丢失进行中对局。",
        "未做正式 WebSocket 压测，200 并发指标仍是设计目标。",
        "未实现观战、好友房、悔棋、禁手规则与赛季/分数衰减。",
        "聊天未做敏感词过滤与举报机制。",
        "真 P2P 仅作为方案对比，未实际接入 WebRTC。",
    ])

    h2("7.3 展望")
    p("后续可引入 Redis 保存匹配队列、会话与限流计数，配合粘性会话/共享状态支持多实例；可增加观战与回放，"
      "复用 moves 表按 seq 重放；可加入禁手规则与 AI 对手；可将评分扩展为赛季与段位；"
      "在安全侧可增加邮箱验证、找回密码、聊天敏感词与更细粒度的审计。若课程要求真 P2P，可在现有系统中增加"
      "“休闲房 WebRTC”模式，排位仍走服务端权威通道。")

    h1("参考文献")
    refs = [
        "[1] Spring Boot Reference Documentation. https://docs.spring.io/spring-boot/",
        "[2] Spring Security Reference. https://docs.spring.io/spring-security/",
        "[3] MySQL 8.0 Reference Manual. https://dev.mysql.com/doc/refman/8.0/en/",
        "[4] RFC 6455: The WebSocket Protocol. IETF, 2011.",
        "[5] Elo, A. E. The Rating of Chessplayers, Past and Present. Arco Publishing, 1978.",
        "[6] Thymeleaf Documentation. https://www.thymeleaf.org/documentation.html",
        "[7] 《Java语言及网络编程》2026-2027 课程报告说明.",
    ]
    for ref in refs:
        para = doc.add_paragraph()
        para.paragraph_format.line_spacing = 1.5
        para.paragraph_format.space_after = Pt(3)
        run = para.add_run(ref)
        set_run_font(run, "宋体", 10.5)

    doc.add_page_break()
    h1("附录 A WebSocket 协议与错误码")
    p("协议采用 JSON 文本帧，信封为 {op, seq, data}。下表列出主要错误码及其触发条件。", indent=True)
    table(
        ["错误码", "触发条件", "客户端处理"],
        [
            ["NOT_YOUR_TURN", "未轮到该玩家", "提示并恢复轮次"],
            ["INVALID_MOVE", "越界/占用/非法落点", "提示重新落子"],
            ["SEQ_MISMATCH", "seq 与服务器不一致", "请求 STATE 重新同步"],
            ["GAME_OVER", "对局已结束仍落子/聊天", "展示结算"],
            ["NOT_IN_GAME", "未 RESUME 就落子", "回到对局页重连"],
            ["RATE_LIMITED", "聊天过频", "提示稍后再试"],
        ],
        [4.8, 8.0, 5.2],
        "表 A-1 主要错误码",
    )

    h1("附录 B 部署与运行")
    p("开发环境：JDK 17、Maven 3.9、MySQL 8.0。先执行 scripts/setup-db.sql 创建 gomoku 库与账号，再运行 scripts/dev.bat。"
      "脚本会自动选择 JDK 17（优先 JAVA17_HOME，其次本机路径，最后 JAVA_HOME），可用 --check 只检查环境。")
    code("""mvn -DskipTests package
java -jar target/gomoku-0.1.0-SNAPSHOT.jar

# 或
mvn -DskipTests package
docker compose up --build""")
    p("生产环境设置 SPRING_PROFILES_ACTIVE=prod；HTTPS 下设置 SESSION_COOKIE_SECURE=true；"
      "反向代理需转发 Upgrade/Connection 头。默认数据库账号只用于开发，生产应改为强口令并限制授权范围。")

    h1("附录 C AI 工程化记录摘要")
    p("项目按“规格→生成→验证→人工审批”的闭环使用 AI：先把需求写成可验收的规格与测试映射（skills/gomoku-spec/SKILL.md），"
      "再让模型生成模块代码，随后执行 JUnit/MockMvc/WebSocket 测试；认证、数据库迁移、WebSocket 鉴权与评分逻辑"
      "必须人工复核后才能合入。仓库 docs/ai 下保存了 prompt 记录、审批门禁与评测报告。")
    p("AI 在这个过程中主要承担样板代码、测试用例与文档初稿，人工负责边界定义、协议设计、缺陷定位与最终取舍。"
      "典型收益是重复性代码与测试编写时间下降；典型风险是“看起来能跑”的实现绕过边界条件，因此测试与审批不可省略。")

    doc.save(OUT)
    build_toc(OUT)
    print("saved", OUT)


NODE = "D:/DSH/resources/runtime/primary-runtime/dependencies/node/bin/node.exe"
CLI = "D:/DSH/resources/app.asar.unpacked/dsh/node_modules/@deepseek-ai/libreoffice-kit/lib/cli.js"
PDF = os.path.join(BASE, "docs", "报告", "Java语言及网络编程课程报告.pdf")
TXT = os.path.join(BASE, "target", "report-pdf.txt")


def normalize(text):
    return re.sub(r"\s+", "", text)


def locate_page(pages, body_start, title):
    needle = normalize(title)
    for i in range(body_start, len(pages)):
        for line in pages[i].splitlines():
            if normalize(line) == needle:
                return i - body_start + 1
    # 标题行可能与表标题/页码排在同一行，退化为行首匹配
    for i in range(body_start, len(pages)):
        for line in pages[i].splitlines():
            n = normalize(line)
            if n.startswith(needle):
                return i - body_start + 1
    return None


def build_toc(docx_path):
    os.makedirs(os.path.dirname(PDF), exist_ok=True)
    if os.path.exists(PDF):
        os.remove(PDF)
    result = subprocess.run(
        [NODE, CLI, "convert", "--input", docx_path, "--output", PDF],
        capture_output=True, text=True)
    if result.returncode != 0:
        print("pdf convert failed:", result.stdout, result.stderr)
        return

    pdftotext = "pdftotext"
    result = subprocess.run([pdftotext, "-enc", "UTF-8", "-layout", PDF, TXT],
                            capture_output=True, text=True)
    if result.returncode != 0:
        print("pdftotext failed:", result.stdout, result.stderr)
        return

    with open(TXT, "r", encoding="utf-8", errors="ignore") as f:
        pages = f.read().split("\f")

    body_start = None
    for i, page in enumerate(pages):
        normalized = normalize(page)
        if "摘要" in normalized and "本报告完成了" in normalized:
            body_start = i
            break
    if body_start is None:
        print("body start not found")
        return

    entries = []
    for level, title in headings:
        page_no = locate_page(pages, body_start, title)
        entries.append((level, title, page_no))

    doc = Document(docx_path)
    field_p = None
    for para in doc.paragraphs:
        if "TOC" in para._p.xml and "instrText" in para._p.xml:
            field_p = para
            break
    if field_p is None:
        print("TOC field not found")
        return

    for level, title, page_no in entries:
        para = doc.add_paragraph()
        field_p._p.addprevious(para._p)
        pf = para.paragraph_format
        if level == 1:
            pf.left_indent = Cm(0.0)
            pf.space_before = Pt(7)
            pf.space_after = Pt(2)
        elif level == 2:
            pf.left_indent = Cm(0.74)
            pf.space_before = Pt(2)
            pf.space_after = Pt(1)
        else:
            pf.left_indent = Cm(1.48)
            pf.space_before = Pt(1)
            pf.space_after = Pt(1)
        pf.line_spacing = 1.25
        pf.tab_stops.add_tab_stop(Cm(15.0), WD_TAB_ALIGNMENT.RIGHT, WD_TAB_LEADER.DOTS)

        run = para.add_run(title)
        set_run_font(run, "宋体", 12 if level == 1 else 11 if level == 2 else 10.5,
                     bold=(level == 1))
        run = para.add_run("\t" + (str(page_no) if page_no else ""))
        set_run_font(run, "宋体", 11 if level == 1 else 10.5, bold=(level == 1))

    field_p._p.getparent().remove(field_p._p)
    doc.save(docx_path)

    if os.path.exists(PDF):
        os.remove(PDF)
    result = subprocess.run(
        [NODE, CLI, "convert", "--input", docx_path, "--output", PDF],
        capture_output=True, text=True)
    if result.returncode != 0:
        print("final pdf convert failed:", result.stdout, result.stderr)
    print("toc entries:", len(entries), "pdf pages:", len(pages))
    refresh_toc(docx_path)


def refresh_toc(docx_path):
    """用最终 PDF 的实际页码修正静态目录（目录条目数量不变，不影响正文分页）。"""
    if not os.path.exists(PDF):
        return
    text_path = os.path.join(BASE, "target", "report-final.txt")
    subprocess.run(["pdftotext", "-enc", "UTF-8", "-layout", PDF, text_path],
                   capture_output=True, text=True)
    with open(text_path, "r", encoding="utf-8", errors="ignore") as f:
        pages = f.read().split("\f")

    body_start = None
    for i, page in enumerate(pages):
        normalized = normalize(page)
        if "摘要" in normalized and "本报告完成了" in normalized:
            body_start = i
            break
    if body_start is None:
        print("refresh: body start not found")
        return

    page_map = {}
    for level, title in headings:
        page_no = locate_page(pages, body_start, title)
        if page_no is not None:
            page_map[title] = page_no

    doc = Document(docx_path)
    updated = 0
    for para in doc.paragraphs:
        text = para.text
        if "\t" not in text:
            continue
        title = text.split("\t", 1)[0]
        page_no = page_map.get(title)
        if page_no is None:
            continue
        runs = para.runs
        for run in runs:
            if "\t" in run.text:
                run.text = "\t" + str(page_no)
                updated += 1
                break
        else:
            if runs:
                runs[-1].text = "\t" + str(page_no)
                updated += 1
    doc.save(docx_path)

    if os.path.exists(PDF):
        os.remove(PDF)
    subprocess.run([NODE, CLI, "convert", "--input", docx_path, "--output", PDF],
                   capture_output=True, text=True)
    print("refresh: updated", updated, "toc entries")


if __name__ == "__main__":
    main()
