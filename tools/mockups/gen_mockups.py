# -*- coding: utf-8 -*-
"""
按 app/src/main/res 里的真实布局、颜色与尺寸令牌，生成 README 用的界面效果图源文件。
每个界面输出一个 412x892 的 HTML（与设计稿、实机 dp 尺寸一致），再由 headless Chrome 截图。

注意：这里只做“渲染源”，不做业务逻辑；文字内容取自 NoteStore 的示例数据，
保证效果图展示的内容与实际首次启动看到的一致。
"""
import datetime as dt
import os
import shutil

ROOT = os.path.dirname(os.path.abspath(__file__))
BUILD = os.path.join(ROOT, "build")

SANS = '"Noto Sans SC","Source Han Sans SC","Microsoft YaHei",sans-serif'
SERIF = '"Noto Serif SC","Source Han Serif SC","Songti SC","SimSun",serif'

# ---------------------------------------------------------------- 设计令牌（与 colors.xml 对齐）
LIGHT = """
  --bg:#F8F5F0; --surface:#FFFFFF; --surface_alt:#FBF8F2;
  --ink:#1F1E1B; --ink_body:#2B2823; --secondary:#5C5A55; --tertiary:#928C84;
  --quaternary:#A39A90; --ghost:#B3ACA3;
  --primary:#4E6B45; --primary_soft:#EAF0E4; --primary_light:#87A878; --on_primary:#FFFFFF;
  --clay:#D89575; --clay_soft:#FBEDE6; --sand:#F1ECE2;
  --divider:#E8E1D4; --stroke:#DFD8CB;
  --review_a:#4E6B45; --review_b:#76916B;
  --photo_a:#D9DBC2; --photo_b:#C2A885;
  --avatar_a:#DBC9AE; --avatar_b:#AEB899;
  --shadow:31,30,27;
"""

DARK = """
  --bg:#171511; --surface:#23201B; --surface_alt:#1E1B16;
  --ink:#F4EFE7; --ink_body:#E7E0D5; --secondary:#A8A096; --tertiary:#8B8378;
  --quaternary:#7C756B; --ghost:#6F6862;
  --primary:#9EBB8B; --primary_soft:#2A3328; --primary_light:#B9CDA6; --on_primary:#171511;
  --clay:#DCA98F; --clay_soft:#3A2B25; --sand:#2C2A24;
  --divider:#2E2A23; --stroke:#322D26;
  --review_a:#2E3A28; --review_b:#3F5138;
  --photo_a:#4A4B3E; --photo_b:#6B5B45;
  --avatar_a:#3A3C33; --avatar_b:#2C3229;
  --shadow:0,0,0;
"""

CSS = """
* { box-sizing: border-box; }
html, body { margin:0; padding:0; }
body {
  width:412px; height:892px; overflow:hidden; display:flex; flex-direction:column;
  background:var(--bg); color:var(--ink);
  font-family:__SANS__; font-size:14px; line-height:1.4;
  -webkit-font-smoothing:antialiased;
}
.serif { font-family:__SERIF__; }
.row { display:flex; align-items:center; }
.row-b { display:flex; align-items:center; justify-content:space-between; }
.col { display:flex; flex-direction:column; }
.spacer { flex:1; }
.status {
  height:28px; display:flex; align-items:flex-end; justify-content:space-between;
  padding:0 22px 3px; font-size:12px; font-weight:600; letter-spacing:.02em;
  font-family:"Inter","Segoe UI",sans-serif;
}
.status .icons { display:flex; align-items:center; gap:5px; }
.bar { height:9px; width:3px; border-radius:1px; background:var(--ink); }
.bat { width:20px; height:10px; border:1.2px solid var(--ink); border-radius:2.5px; position:relative; opacity:.9; }
.bat::after { content:""; position:absolute; left:1.4px; top:1.4px; bottom:1.4px; width:11px; background:var(--ink); border-radius:1px; }
.bat::before { content:""; position:absolute; right:-3.4px; top:2.6px; width:2px; height:3.6px; background:var(--ink); border-radius:0 1px 1px 0; }

.content { padding:12px 20px 0; }
.meta { font-size:12px; color:var(--tertiary); }
.caption { font-size:11px; color:var(--tertiary); }
.display { font-family:__SERIF__; font-size:26px; font-weight:700; line-height:1.3; color:var(--ink); letter-spacing:.005em; }
.section { font-family:__SERIF__; font-size:18px; font-weight:700; color:var(--ink); }
.title20 { font-family:__SERIF__; font-size:20px; font-weight:700; color:var(--ink); }
.num { font-size:22px; font-weight:500; color:var(--ink); }
.avatar {
  border-radius:9999px;
  background:linear-gradient(135deg, var(--avatar_a), var(--avatar_b));
  flex:none;
}
.icon-btn {
  width:36px; height:36px; border-radius:12px; background:var(--surface);
  border:1px solid var(--divider); display:flex; align-items:center; justify-content:center;
  color:var(--secondary); flex:none;
}
.icon-btn.soft { background:var(--sand); border-color:transparent; }
.icon-btn.plain { background:transparent; border-color:transparent; }
.tile-btn {
  width:34px; height:34px; border-radius:12px; background:var(--sand);
  display:flex; align-items:center; justify-content:center; color:var(--tertiary); flex:none;
}
.btn {
  border-radius:12px; height:32px; min-width:64px; padding:0 16px;
  display:inline-flex; align-items:center; justify-content:center;
  font-size:12px; font-weight:500; flex:none;
}
.btn.primary { background:var(--primary); color:var(--on_primary); }
.btn.secondary { background:var(--surface); color:var(--secondary); border:1px solid var(--stroke); }
.btn.small { height:28px; min-width:0; padding:0 10px; font-size:11px; }
.chip {
  background:var(--primary_soft); color:var(--primary); border-radius:11px;
  padding:4px 8px; font-size:11px; font-weight:500; white-space:nowrap;
}
.chip.sand { background:var(--sand); color:var(--secondary); }
.chip.outline { background:transparent; border:1px dashed var(--stroke); color:var(--tertiary); }
.card { background:var(--surface); border-radius:20px; border:1px solid var(--divider); padding:18px; }
.note-card {
  background:var(--surface); border-radius:18px; padding:16px; margin-bottom:12px;
  box-shadow:0 1px 2px rgba(var(--shadow),.05), 0 4px 12px rgba(var(--shadow),.035);
}
.divider { height:1px; background:var(--divider); }

/* 底部胶囊导航 */
.nav {
  margin:8px 16px 16px; margin-top:auto; height:62px; padding:4px; display:flex; flex:none;
  background:var(--surface); border:1px solid var(--divider); border-radius:36px;
  box-shadow:0 6px 18px rgba(var(--shadow),.07);
}
.tab {
  flex:1; display:flex; flex-direction:column; align-items:center; justify-content:center;
  gap:3px; border-radius:26px; color:var(--tertiary);
  font-size:10px; font-weight:500; letter-spacing:.04em;
}
.tab.on { background:var(--primary); color:var(--on_primary); }

/* 图片占位：与 bg_photo 的同色系渐变一致 */
.photo {
  border-radius:14px; overflow:hidden; position:relative;
  background:linear-gradient(150deg, var(--photo_a), var(--photo_b));
}
.photo .soft { position:absolute; border-radius:9999px; filter:blur(18px); }

/* 时间轴 */
.tl { display:flex; margin-bottom:16px; }
.tl .rail { width:17px; padding-top:6px; flex:none; }
.tl .dot { width:7px; height:7px; border-radius:9999px; background:var(--primary); }
.tl .time { font-size:11px; font-weight:500; color:var(--tertiary); letter-spacing:.04em; }
.tl .ttl { font-size:14px; font-weight:500; color:var(--ink); margin-top:3px; }
.tl .ex { font-size:13px; color:var(--secondary); margin-top:3px; line-height:1.55; }
.group-label { font-size:12px; font-weight:500; color:var(--quaternary); letter-spacing:.04em; margin:2px 0 12px; }
.hl { color:var(--primary); font-weight:700; }

/* 周历 */
.week { display:flex; margin-top:18px; }
.week .col-d { flex:1; display:flex; flex-direction:column; align-items:center; gap:6px; }
.week .wd { font-size:10px; font-weight:500; color:var(--quaternary); }
.week .dn {
  width:34px; height:34px; border-radius:9999px; display:flex; align-items:center; justify-content:center;
  font-size:15px; color:var(--ink_body);
}
.week .col-d.on .wd { color:var(--primary); }
.week .col-d.on .dn { background:var(--primary); color:var(--on_primary); font-weight:500; }
.week .col-d.out .dn { color:var(--ghost); }

/* 弹框 */
.scrim { position:absolute; inset:0; background:rgba(23,21,17,.42); }
.dialog {
  position:absolute; left:10px; right:10px; top:50%; transform:translateY(-50%);
  background:var(--surface); border:1px solid var(--divider); border-radius:22px;
  box-shadow:0 18px 48px rgba(0,0,0,.28);
}
/* 月历：窗口 356dp（含 10dp 外边距）→ 卡片 336dp，内容宽 300dp，7 列各 42.8dp */
.cal-dialog {
  position:absolute; left:50%; top:50%; transform:translate(-50%,-50%);
  width:336px; padding:20px 18px 16px; background:var(--surface);
  border:1px solid var(--divider); border-radius:22px;
  box-shadow:0 18px 48px rgba(0,0,0,.28);
}
.cal-week { display:grid; grid-template-columns:repeat(7,1fr); height:28px; align-items:center; }
.cal-week > div { text-align:center; font-size:11px; font-weight:500; color:var(--quaternary); }
.cal-grid { display:grid; grid-template-columns:repeat(7,1fr); margin-top:2px; }
.cal-cell { height:46px; display:flex; flex-direction:column; align-items:center; justify-content:flex-start; }
.cal-num {
  width:32px; height:32px; margin-top:2px; border-radius:9999px;
  display:flex; align-items:center; justify-content:center; font-size:14px; color:var(--ink_body);
}
.cal-cell.on .cal-num { background:var(--primary); color:var(--on_primary); font-weight:500; }
.cal-cell.out .cal-num { color:var(--ghost); }
.cal-cell.future { opacity:.42; }
.cal-dot { width:4px; height:4px; border-radius:9999px; background:var(--primary_light); margin-top:3px; }
.cal-cell.on .cal-dot { background:var(--on_primary); }
.cal-dot.hide { opacity:0; }

.bar-col { flex:1; display:flex; flex-direction:column; align-items:center; justify-content:flex-end; gap:7px; }
.bar-fill { width:22px; border-radius:4px 4px 2px 2px; background:var(--primary); }
.bar-cap { font-size:10px; color:var(--tertiary); }
"""


def icon(name, size=18, sw=1.7):
    paths = {
        "pen": '<path d="M3.5 16.2 4 19l2.8.5L17.6 8.7l-3.3-3.3L3.5 16.2Z"/><path d="m13.2 6.6 3.3 3.3"/>',
        "camera": '<rect x="2.7" y="6.4" width="14.6" height="11" rx="2.6"/><circle cx="10" cy="11.9" r="3.1"/><path d="M7.4 6.4 8.4 4.2h3.2l1 2.2"/>',
        "tag": '<path d="M10.6 3.2H16a.8.8 0 0 1 .8.8v5.4a1.4 1.4 0 0 1-.4 1l-6.6 6.6a1.4 1.4 0 0 1-2 0l-4.4-4.4a1.4 1.4 0 0 1 0-2l6.6-6.6a1.4 1.4 0 0 1 1-.4Z"/><circle cx="13.2" cy="6.8" r="1.1"/>',
        "search": '<circle cx="9.2" cy="9.2" r="5.4"/><path d="m13.2 13.2 4.1 4.1"/>',
        "calendar": '<rect x="3.2" y="4.6" width="14.2" height="12.6" rx="2.4"/><path d="M3.2 8.2h14.2M6.9 3v3.2M13.6 3v3.2"/>',
        "left": '<path d="m12.2 4.8-5.2 5.2 5.2 5.2"/>',
        "right": '<path d="m7.8 4.8 5.2 5.2-5.2 5.2"/>',
        "down": '<path d="m5.2 8 4.8 4.8L14.8 8"/>',
        "lefts": '<path d="m9.6 4.6-5 5.2 5 5.2"/><path d="m15.2 4.6-5 5.2 5 5.2"/>',
        "rights": '<path d="m10.4 4.6 5 5.2-5 5.2"/><path d="m4.8 4.6 5 5.2-5 5.2"/>',
        "sun": '<circle cx="10" cy="10" r="3.5"/><path d="M10 2.6v1.6M10 15.8v1.6M2.6 10h1.6M15.8 10h1.6M4.8 4.8l1.1 1.1M14.1 14.1l1.1 1.1M15.2 4.8l-1.1 1.1M5.9 14.1l-1.1 1.1"/>',
        "clock": '<circle cx="10" cy="10" r="7.2"/><path d="M10 6.2V10l2.7 1.8"/>',
        "user": '<circle cx="10" cy="7" r="3.1"/><path d="M4.3 16.6a5.8 5.8 0 0 1 11.4 0"/>',
        "trash": '<path d="M3.8 6.2h12.4M8 6.2V4.4h4v1.8M5.6 6.2l.8 10.1a1.4 1.4 0 0 0 1.4 1.3h4.4a1.4 1.4 0 0 0 1.4-1.3l.8-10.1"/>',
        "image": '<rect x="3" y="4.4" width="14" height="11.8" rx="2.4"/><circle cx="7.4" cy="8.4" r="1.2"/><path d="m4.4 14.4 3.5-3.6 3 3 2-2 2.7 2.6"/>',
        "list": '<path d="M7.6 6h8.6M7.6 10h8.6M7.6 14h8.6"/><circle cx="4.4" cy="6" r=".9" fill="currentColor" stroke="none"/><circle cx="4.4" cy="10" r=".9" fill="currentColor" stroke="none"/><circle cx="4.4" cy="14" r=".9" fill="currentColor" stroke="none"/>',
        "text": '<path d="M4.6 5.4h10.8M10 5.4v9.6M7.6 15h4.8"/>',
        "close": '<path d="m5.4 5.4 9.2 9.2M14.6 5.4l-9.2 9.2"/>',
        "more": '<circle cx="4.6" cy="10" r="1.5" fill="currentColor" stroke="none"/><circle cx="10" cy="10" r="1.5" fill="currentColor" stroke="none"/><circle cx="15.4" cy="10" r="1.5" fill="currentColor" stroke="none"/>',
        "bell": '<path d="M6 8.6a4 4 0 0 1 8 0c0 3 .9 4.4 1.6 5.2H4.4C5.1 13 6 11.6 6 8.6Z"/><path d="M8.6 16.2a1.6 1.6 0 0 0 2.8 0"/>',
        "moon": '<path d="M15.2 12.4A6.2 6.2 0 0 1 7.6 4.8 6.2 6.2 0 1 0 15.2 12.4Z"/>',
        "cloud": '<path d="M6.4 14.6h7.4a2.7 2.7 0 0 0 .3-5.4 4.2 4.2 0 0 0-8-1.1 2.9 2.9 0 0 0 .3 6.5Z"/>',
        "info": '<circle cx="10" cy="10" r="7.2"/><path d="M10 9v4.6"/><circle cx="10" cy="6.6" r=".9" fill="currentColor" stroke="none"/>',
        "arrow": '<path d="M3.8 10h11.4m-4-4 4 4-4 4"/>',
        "check": '<circle cx="10" cy="10" r="7.2"/><path d="m6.8 10.2 2.2 2.2 4.2-4.6"/>',
        "back": '<path d="M12.4 4.6 6.8 10l5.6 5.4"/>',
        "plus": '<path d="M10 4.6v10.8M4.6 10h10.8"/>',
    }
    return (
        '<svg width="%d" height="%d" viewBox="0 0 20 20" fill="none" stroke="currentColor" '
        'stroke-width="%s" stroke-linecap="round" stroke-linejoin="round">%s</svg>'
        % (size, size, sw, paths[name])
    )


def status_bar(dark=False):
    return """
  <div class="status">
    <div>9:41</div>
    <div class="icons">
      <div style="display:flex;align-items:flex-end;gap:1.5px">
        <div class="bar" style="height:4px"></div>
        <div class="bar" style="height:6px"></div>
        <div class="bar" style="height:8px"></div>
        <div class="bar" style="height:10px"></div>
      </div>
      <svg width="15" height="11" viewBox="0 0 15 11" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round">
        <path d="M1.4 3.8a9 9 0 0 1 12.2 0M3.9 6.4a5.6 5.6 0 0 1 7.2 0"/>
        <circle cx="7.5" cy="9.2" r="0.9" fill="currentColor" stroke="none"/>
      </svg>
      <div class="bat"></div>
    </div>
  </div>"""


def nav(active="today"):
    items = [("today", "sun", "今日"), ("timeline", "clock", "时光"), ("profile", "user", "我的")]
    out = ['<div class="nav">']
    for key, ic, label in items:
        on = " on" if key == active else ""
        out.append('<div class="tab%s">%s<span>%s</span></div>' % (on, icon(ic, 18), label))
    out.append("</div>")
    return "".join(out)


def _leaves(items, blur=2.6):
    """一片叶子的轮廓：两段对称曲线，尖端收口，比纯椭圆更像植物"""
    out = ['<g filter="url(#g3)">']
    for x, y, rx, ry, ang, color, op in items:
        d = ("M %d 0 C %d %d, %d %d, %d 0 C %d %d, %d %d, %d 0 Z"
             % (-rx, -rx, -int(ry * 0.92), int(rx * 0.3), -ry, rx,
                int(rx * 0.3), ry, -rx, int(ry * 0.92), -rx))
        out.append('<path d="%s" fill="%s" opacity="%s" '
                   'transform="translate(%d %d) rotate(%d)"/>' % (d, color, op, x, y, ang))
    out.append("</g>")
    return "".join(out)


def photo(h, extra="", radius=None):
    """记录配图的占位：与 bg_photo 同色系的暖调枝叶画面（真实 App 中为用户照片）"""
    r = "border-radius:%s;" % radius if radius is not None else ""
    leaves = _leaves([
        (18, 26, 74, 32, -16, "#8E9A6E", ".95"),
        (104, 12, 58, 24, -6, "#A9AF7F", ".86"),
        (162, 50, 48, 20, 14, "#7B875D", ".68"),
        (0, 112, 78, 40, -8, "#6C7550", ".92"),
        (92, 132, 58, 26, 18, "#879468", ".72"),
        (352, 48, 66, 30, 24, "#7C8A5E", ".82"),
        (372, 150, 72, 38, 6, "#6C7550", ".72"),
        (268, 6, 54, 22, 10, "#95A173", ".6"),
    ])
    return """
  <div class="photo" style="height:%dpx;%s%s">
    <svg width="100%%" height="100%%" viewBox="0 0 400 300" preserveAspectRatio="xMidYMid slice"
         style="position:absolute;inset:0;display:block">
      <defs>
        <linearGradient id="sky" x1="0" y1="0" x2="0.3" y2="1">
          <stop offset="0" stop-color="#F0EAD2"/>
          <stop offset="0.5" stop-color="#DACFB0"/>
          <stop offset="1" stop-color="#A89571"/>
        </linearGradient>
        <filter id="g3" x="-30%%" y="-30%%" width="160%%" height="160%%">
          <feGaussianBlur stdDeviation="2.4"/>
        </filter>
        <filter id="g12" x="-40%%" y="-40%%" width="180%%" height="180%%">
          <feGaussianBlur stdDeviation="11"/>
        </filter>
        <filter id="g18" x="-60%%" y="-60%%" width="220%%" height="220%%">
          <feGaussianBlur stdDeviation="20"/>
        </filter>
        <radialGradient id="vg" cx="0.5" cy="0.44" r="0.76">
          <stop offset="0.5" stop-color="#000000" stop-opacity="0"/>
          <stop offset="1" stop-color="#2A2418" stop-opacity="0.34"/>
        </radialGradient>
      </defs>

      <rect width="400" height="300" fill="url(#sky)"/>
      <circle cx="298" cy="66" r="46" fill="#FFF8E4" opacity="0.7" filter="url(#g18)"/>
      %s
      <g stroke="#4A4630" stroke-width="2.6" fill="none" opacity="0.30" filter="url(#g3)">
        <path d="M-8 196 C 62 168, 96 128, 148 104"/>
        <path d="M64 172 C 82 156, 96 146, 122 140"/>
        <path d="M330 118 C 356 106, 374 108, 398 96"/>
      </g>
      <g filter="url(#g12)" opacity="0.5">
        <ellipse cx="210" cy="292" rx="180" ry="52" fill="#5D5A42"/>
      </g>
      <rect width="400" height="300" fill="url(#vg)"/>
    </svg>
  </div>""" % (h, r, extra, leaves)


def photo_tall(h=864):
    """全屏看图用的竖构图占位（与配图同一套暖调，避免拉伸失真）"""
    leaves = _leaves([
        (10, 46, 132, 56, -14, "#8B9769", ".95"),
        (170, 18, 118, 46, -4, "#A6AC7C", ".85"),
        (300, 64, 118, 52, 18, "#79855B", ".9"),
        (48, 190, 110, 44, 10, "#93A071", ".7"),
        (330, 210, 96, 40, -12, "#6C7550", ".66"),
        (0, 320, 120, 52, -6, "#879468", ".55"),
        (400, 420, 110, 46, 14, "#6C7550", ".45"),
    ])
    return """
  <div style="position:absolute;inset:0;overflow:hidden;background:linear-gradient(170deg,#9AA189 0%%,#CDBF9F 46%%,#7A6547 100%%)">
    <svg width="100%%" height="100%%" viewBox="0 0 400 800" preserveAspectRatio="xMidYMid slice"
         style="position:absolute;inset:0;display:block">
      <defs>
        <linearGradient id="ssky" x1="0" y1="0" x2="0.18" y2="1">
          <stop offset="0" stop-color="#F1EACF"/>
          <stop offset="0.38" stop-color="#E0D4B4"/>
          <stop offset="0.72" stop-color="#B7A47C"/>
          <stop offset="1" stop-color="#7E6C4A"/>
        </linearGradient>
        <linearGradient id="haze" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stop-color="#FFF6DE" stop-opacity="0"/>
          <stop offset="0.55" stop-color="#FFF6DE" stop-opacity="0.42"/>
          <stop offset="1" stop-color="#FFF6DE" stop-opacity="0"/>
        </linearGradient>
        <filter id="g3" x="-30%%" y="-30%%" width="160%%" height="160%%">
          <feGaussianBlur stdDeviation="2.6"/>
        </filter>
        <filter id="g12" x="-40%%" y="-40%%" width="180%%" height="180%%">
          <feGaussianBlur stdDeviation="12"/>
        </filter>
        <filter id="g18" x="-60%%" y="-60%%" width="220%%" height="220%%">
          <feGaussianBlur stdDeviation="22"/>
        </filter>
        <radialGradient id="tvg" cx="0.5" cy="0.44" r="0.72">
          <stop offset="0.42" stop-color="#000000" stop-opacity="0"/>
          <stop offset="1" stop-color="#241F14" stop-opacity="0.48"/>
        </radialGradient>
      </defs>

      <rect width="400" height="800" fill="url(#ssky)"/>
      <circle cx="286" cy="206" r="62" fill="#FFF8E4" opacity="0.66" filter="url(#g18)"/>
      %s
      <g stroke="#4A4630" stroke-width="3" fill="none" opacity="0.26" filter="url(#g3)">
        <path d="M-10 300 C 78 274, 120 214, 196 186"/>
        <path d="M112 262 C 130 240, 148 228, 176 220"/>
        <path d="M410 330 C 340 306, 304 272, 256 260"/>
        <path d="M300 470 C 330 452, 352 448, 386 430"/>
      </g>
      <rect y="300" width="400" height="220" fill="url(#haze)"/>
      <g filter="url(#g18)" opacity="0.55">
        <ellipse cx="200" cy="800" rx="320" ry="140" fill="#5C5738"/>
        <ellipse cx="40" cy="720" rx="200" ry="96" fill="#7E7048" opacity="0.7"/>
      </g>
      <rect width="400" height="800" fill="url(#tvg)"/>
    </svg>
  </div>""" % leaves


def avatar(size):
    return '<div class="avatar" style="width:%dpx;height:%dpx"></div>' % (size, size)


# ================================================================ 01 今日
def screen_today():
    card2 = """
  <div class="note-card">
    <div class="row-b">
      <div class="time" style="font-size:11px;font-weight:500;color:var(--tertiary);letter-spacing:.04em">14:30</div>
      <div class="chip">愉悦</div>
    </div>
    <div style="font-size:15px;line-height:1.72;color:var(--ink_body);margin-top:10px">楼下的桂花开了，风一过，整条街都是甜的。</div>
    %s
  </div>""" % photo(168)

    return """
<div class="content">
  <div class="row-b">
    <div>
      <div class="meta" style="letter-spacing:.06em">10月22日 · 星期四</div>
      <div class="display" style="margin-top:6px">下午好，随心</div>
    </div>
    <div class="avatar" style="width:46px;height:46px"></div>
  </div>

  <div class="card" style="margin-top:20px;padding:18px;box-shadow:0 2px 10px rgba(var(--shadow),.05)">
    <div class="row" style="gap:10px">
      <span style="color:var(--primary);display:flex">%s</span>
      <span style="font-size:15px;color:var(--tertiary)">此刻，想记下什么？</span>
    </div>
    <div class="row" style="margin-top:14px">
      <div class="tile-btn">%s</div>
      <div class="tile-btn" style="margin-left:8px">%s</div>
      <div class="spacer"></div>
      <div class="btn primary">记录</div>
    </div>
  </div>

  <div class="row-b" style="margin-top:24px;align-items:flex-end">
    <div class="section">今日</div>
    <div class="meta">3 条 · 268 字</div>
  </div>

  <div style="margin-top:12px">
    <div class="note-card">
      <div class="row-b">
        <div style="font-size:11px;font-weight:500;color:var(--tertiary);letter-spacing:.04em">09:12</div>
        <div class="chip">平静</div>
      </div>
      <div style="font-size:15px;line-height:1.72;color:var(--ink_body);margin-top:10px">早起煮了一壶茶，把上周没读完的书翻到最后三章。窗外起了雾。</div>
    </div>
    %s
    <div class="note-card">
      <div class="row-b">
        <div style="font-size:11px;font-weight:500;color:var(--tertiary);letter-spacing:.04em">21:06</div>
        <div class="chip">平静</div>
      </div>
      <div style="font-size:15px;line-height:1.72;color:var(--ink_body);margin-top:10px">把今天的照片洗出来贴上，纸的味道很好闻。</div>
    </div>
  </div>
</div>
%s""" % (icon("pen", 18), icon("camera", 16), icon("tag", 16), card2, nav("today"))


# ================================================================ 02 写记录
def screen_editor():
    return """
<div style="padding:12px 20px 0">
  <div class="row-b">
    <div class="icon-btn plain" style="color:var(--ink)">%s</div>
    <div class="meta">草稿 · 自动保存</div>
    <div class="btn primary" style="height:36px;min-width:72px;padding:0 20px;font-size:13px">保存</div>
  </div>
</div>

<div style="padding:16px 20px 0">
  <div class="serif" style="font-size:26px;font-weight:700;color:var(--ink);line-height:1.35">桂花开了</div>
  <div class="meta" style="margin-top:8px;letter-spacing:.02em">10月21日 星期三 · 21:12</div>
  <div style="margin-top:14px;font-size:15px;line-height:2.05;color:var(--ink_body)">
    风一过，整条街都是甜的。<br/>
    在小区门口站了很久，看路灯把树影投在地上。<br/>
    想起小时候院子里也有一棵，那时不知道它叫桂花，只知道秋天会香。
  </div>

  <div class="row" style="margin-top:18px;gap:10px">
    <div style="width:96px;height:96px;position:relative;flex:none">
      %s
      <div style="position:absolute;right:4px;top:4px;width:20px;height:20px;border-radius:9999px;background:rgba(31,30,27,.42);display:flex;align-items:center;justify-content:center;color:#fff">%s</div>
    </div>
    <div style="width:96px;height:96px;flex:none">%s</div>
    <div style="width:96px;height:96px;border-radius:14px;border:1px dashed var(--stroke);display:flex;flex-direction:column;align-items:center;justify-content:center;gap:6px;color:var(--tertiary)">
      %s
      <span style="font-size:11px">继续添加</span>
    </div>
  </div>

  <div class="row" style="margin-top:18px;gap:8px">
    <div class="chip sand">散步</div>
    <div class="chip sand">秋天</div>
    <div class="chip outline">＋ 标签</div>
  </div>

  <div class="row" style="margin-top:20px;gap:6px;color:var(--primary_light)">
    %s
    <span class="caption" style="letter-spacing:.02em">已自动保存 · 68 字</span>
  </div>
</div>

<div class="spacer"></div>
<div class="row" style="height:72px;background:var(--surface);padding:0 16px;box-shadow:0 -6px 18px rgba(var(--shadow),.05)">
  <div class="tile-btn" style="width:40px;height:40px;color:var(--primary);background:var(--sand)">%s</div>
  <div class="spacer"></div>
  <div class="row" style="gap:14px;color:var(--secondary)">
    <div style="width:40px;height:40px;display:flex;align-items:center;justify-content:center">%s</div>
    <div style="width:40px;height:40px;display:flex;align-items:center;justify-content:center">%s</div>
    <div style="width:40px;height:40px;display:flex;align-items:center;justify-content:center">%s</div>
  </div>
</div>""" % (icon("back", 18),
             photo(96, radius="12px"), icon("close", 12, 2.2), photo(96, radius="12px"), icon("plus", 20),
             icon("check", 13), icon("text", 20), icon("list", 20), icon("image", 20), icon("trash", 20))


# ================================================================ 03 记录详情
def screen_detail():
    return """
<div style="padding:12px 20px 0">
  <div class="row-b">
    <div class="icon-btn plain" style="color:var(--ink)">%s</div>
    <div class="row" style="gap:8px">
      <div class="icon-btn plain" style="color:var(--primary)">%s</div>
      <div class="icon-btn soft">%s</div>
    </div>
  </div>
</div>

<div style="padding:10px 22px 0">
  <div class="meta" style="letter-spacing:.06em">10月21日 · 星期三</div>
  <div class="serif" style="font-size:25px;font-weight:700;line-height:1.4;margin-top:10px">桂花开了</div>
  <div class="row" style="margin-top:10px;gap:8px">
    <span style="font-size:11px;font-weight:500;color:var(--tertiary);letter-spacing:.04em">21:12</span>
    <span style="width:3px;height:3px;border-radius:9999px;background:var(--stroke)"></span>
    <span class="caption">68 字</span>
  </div>
  <div class="row" style="margin-top:14px;gap:6px">
    <div class="chip sand">愉悦</div>
    <div class="chip sand">散步</div>
    <div class="chip sand">秋天</div>
  </div>

  <div class="serif" style="margin-top:18px;font-size:16px;line-height:2.15;color:var(--ink_body)">
    风一过，整条街都是甜的。<br/>
    在小区门口站了很久，看路灯把树影投在地上。想起小时候院子里也有一棵，那时不知道它叫桂花，只知道秋天会香。
  </div>

  <div class="caption" style="margin-top:20px">共 2 张 · 点开可放大</div>
  <div style="margin-top:10px;display:flex;flex-direction:column;gap:10px">
    %s
    %s
  </div>

  <div class="divider" style="margin-top:26px"></div>
  <div class="serif" style="margin-top:14px;text-align:right;font-size:12px;color:var(--ghost);letter-spacing:.04em">—— 随心记</div>
</div>""" % (icon("back", 18), icon("pen", 18), icon("more", 18), photo(210), photo(150))


# ================================================================ 04 时光（月视图）
def screen_timeline():
    def day_cell(label, num, state=""):
        return ('<div class="col-d %s"><div class="wd">%s</div><div class="dn">%s</div></div>'
                % (state, label, num))

    week = "".join([
        day_cell("一", 19), day_cell("二", 20), day_cell("三", 21),
        day_cell("四", 22, "on"), day_cell("五", 23), day_cell("六", 24), day_cell("日", 25),
    ])

    item = """
  <div class="tl">
    <div class="rail"><div class="dot"></div></div>
    <div style="flex:1">
      <div class="time">%s</div>
      <div class="ttl">%s</div>
      <div class="ex">%s</div>
    </div>
  </div>"""

    return """
<div class="content">
  <div class="row-b">
    <div class="display">时光</div>
    <div class="icon-btn">%s</div>
  </div>

  <div class="row" style="margin-top:18px">
    <div style="width:28px;color:var(--secondary);display:flex;justify-content:center">%s</div>
    <div class="row" style="gap:5px;margin:0 2px">
      <span class="serif" style="font-size:16px;font-weight:700;letter-spacing:.02em">2026年10月</span>
      <span style="color:var(--tertiary);display:flex">%s</span>
    </div>
    <div style="width:28px;color:var(--secondary);display:flex;justify-content:center">%s</div>
    <div class="spacer"></div>
    <div class="icon-btn">%s</div>
    <div class="chip" style="margin-left:6px;padding:5px 10px;font-size:11px">今天</div>
  </div>

  <div class="week">%s</div>

  <div class="card" style="margin-top:20px;display:flex;padding:18px">
    <div style="flex:1">
      <div class="num">12</div>
      <div class="caption" style="margin-top:3px">连续记录 · 天</div>
    </div>
    <div style="width:1px;height:30px;background:var(--divider)"></div>
    <div style="flex:1;margin-left:16px">
      <div class="num">28</div>
      <div class="caption" style="margin-top:3px">本月记录 · 条</div>
    </div>
    <div style="width:1px;height:30px;background:var(--divider)"></div>
    <div style="flex:1;margin-left:16px">
      <div class="num">4.2万</div>
      <div class="caption" style="margin-top:3px">累计字数</div>
    </div>
  </div>

  <div style="margin-top:22px">
    <div class="group-label">10月22日 · 周四</div>
    %s
    %s
    %s
    <div class="group-label" style="margin-top:6px">10月21日 · 周三</div>
    %s
    <div class="row" style="justify-content:center;gap:6px;padding:8px;color:var(--tertiary);margin-top:4px">
      <span style="font-size:12px;font-weight:500">查看更早的记录</span>
      <span style="display:flex">%s</span>
    </div>
  </div>
</div>
%s""" % (
        icon("search", 18),
        icon("left", 18), icon("down", 11), icon("right", 18),
        icon("calendar", 18),
        week,
        item % ("09:12", "早起的三章书", "一壶茶，三章书，雾很厚。"),
        item % ("14:30", "", "楼下的桂花开了，风一过，整条街都是甜的。"),
        item % ("21:06", "", "把今天的照片洗出来贴上，纸的味道很好闻。"),
        item % ("21:12", "桂花开了", "风一过，整条街都是甜的。"),
        icon("down", 12),
        nav("timeline"),
    )


# ================================================================ 05 月历选日
def screen_calendar():
    """与 MonthGrid.build 完全一致：周一开头、固定 6 行；锚定 2026 年 10 月（今天 10/22）。"""
    year, month = 2026, 10
    today = dt.date(2026, 10, 22)
    first = dt.date(year, month, 1)
    start = first - dt.timedelta(days=first.weekday())     # 10/1 为周四 → 回退到 9/28
    marked = set(range(11, 23))                            # 首次启动示例数据：10/11–10/22 连续 12 天

    cells = ""
    for i in range(42):
        d = start + dt.timedelta(days=i)
        in_month = d.month == month
        cls = "cal-cell"
        if not in_month:
            cls += " out"
        if d == today:
            cls += " on"                                   # 选中（今天）
        elif d > today:
            cls += " future"                                # 未来的日子弱化且不可点
        dot_cls = "cal-dot" if (in_month and d.day in marked) else "cal-dot hide"
        cells += ('<div class="%s"><div class="cal-num">%d</div>'
                  '<div class="%s"></div></div>' % (cls, d.day, dot_cls))

    week = "".join('<div>%s</div>' % w for w in "一二三四五六日")

    overlay = """
<div class="scrim"></div>
<div class="cal-dialog">
  <div class="row-b">
    <div class="serif" style="font-size:18px;font-weight:700">选择日期</div>
    <div style="font-size:13px;color:var(--tertiary)">取消</div>
  </div>

  <div class="row" style="margin-top:12px">
    <div style="width:30px;display:flex;justify-content:center;color:var(--tertiary)">%(py)s</div>
    <div style="width:32px;display:flex;justify-content:center;color:var(--secondary)">%(pm)s</div>
    <div class="serif" style="flex:1;text-align:center;font-size:16px;font-weight:700;letter-spacing:.02em">2026年10月</div>
    <div style="width:32px;display:flex;justify-content:center;color:var(--secondary);opacity:.26">%(nm)s</div>
    <div style="width:30px;display:flex;justify-content:center;color:var(--tertiary);opacity:.26">%(ny)s</div>
  </div>

  <div class="cal-week" style="margin-top:10px">%(week)s</div>
  <div class="cal-grid">%(cells)s</div>

  <div class="row-b" style="margin-top:10px">
    <div style="font-size:11px;color:var(--ghost)">有记录的日子带小圆点</div>
    <div class="chip" style="height:32px;display:flex;align-items:center;padding:0 12px;font-size:12px;border-radius:11px">回到今天</div>
  </div>
</div>""" % {
        "py": icon("lefts", 16, 1.6), "pm": icon("left", 16, 1.6),
        "nm": icon("right", 16, 1.6), "ny": icon("rights", 16, 1.6),
        "week": week, "cells": cells,
    }

    return """
<div style="position:relative;height:864px;overflow:hidden">
  %s
  <div style="position:absolute;left:0;right:0;bottom:0">%s</div>
  %s
</div>""" % (screen_timeline().replace(nav("timeline"), ""), nav("timeline"), overlay)


# ================================================================ 06 搜索
def screen_search():
    hit = """
  <div class="tl">
    <div class="rail"><div class="dot"></div></div>
    <div style="flex:1">
      <div class="time">14:30</div>
      <div class="ttl"><span class="hl">桂花</span>开了</div>
      <div class="ex">风一过，整条街都是甜的。</div>
    </div>
  </div>"""
    hit2 = """
  <div class="tl">
    <div class="rail"><div class="dot"></div></div>
    <div style="flex:1">
      <div class="time">21:12</div>
      <div class="ttl">散步</div>
      <div class="ex">楼下的<span class="hl">桂花</span>开了，风一过，整条街都是甜的。</div>
    </div>
  </div>"""

    return """
<div class="content">
  <div class="row-b">
    <div class="display">时光</div>
    <div class="icon-btn">%s</div>
  </div>

  <div class="row" style="margin-top:14px;height:44px;background:var(--surface_alt);border:1px solid var(--stroke);border-radius:12px;padding:0 4px 0 12px;gap:8px">
    <span style="color:var(--tertiary);display:flex">%s</span>
    <div style="flex:1;font-size:14px;color:var(--ink)">桂花</div>
    <div style="font-size:12px;color:var(--tertiary);padding:0 10px">取消</div>
  </div>

  <div style="margin-top:16px;font-size:12px;font-weight:500;color:var(--tertiary)">找到 2 条 · 含「桂花」</div>

  <div style="margin-top:18px">
    <div class="group-label">10月22日 · 周四</div>
    %s
    <div class="group-label" style="margin-top:6px">10月21日 · 周三</div>
    %s
    <div class="row" style="justify-content:center;gap:6px;padding:8px;color:var(--tertiary);margin-top:4px">
      <span style="font-size:12px;font-weight:500">查看更早的记录</span>
      <span style="display:flex">%s</span>
    </div>
  </div>
</div>
%s""" % (icon("search", 18), icon("search", 16), hit, hit2,
             icon("down", 12), nav("timeline"))


# ================================================================ 07 我的
def screen_profile():
    def setting(ic, name, value):
        return """
    <div class="row" style="padding:14px 14px 14px 16px">
      <span style="color:var(--secondary);display:flex">%s</span>
      <div style="flex:1;margin-left:12px;font-size:14px;color:var(--ink)">%s</div>
      <div style="font-size:12px;color:var(--quaternary)">%s</div>
      <span style="margin-left:6px;color:var(--quaternary);display:flex">%s</span>
    </div>""" % (icon(ic, 18), name, value, icon("right", 14))

    return """
<div class="content">
  <div class="row">
    <div class="avatar" style="width:62px;height:62px"></div>
    <div style="margin-left:14px;flex:1">
      <div class="title20">随心</div>
      <div class="meta" style="margin-top:5px">286 条记录 · 连续 12 天 · 4.2 万字</div>
    </div>
  </div>

  <div style="margin-top:18px;border-radius:20px;padding:18px;background:linear-gradient(135deg,var(--review_a),var(--review_b));box-shadow:0 8px 20px rgba(var(--shadow),.12)">
    <div style="font-size:11px;font-weight:500;letter-spacing:.14em;color:var(--primary_soft)">本周回顾</div>
    <div style="margin-top:10px;font-size:16px;font-weight:500;line-height:1.55;color:#FFFFFF">你记录了 5 天，写下 9 条、1,240 字。周四写得最多，那天有 3 条。</div>
    <div class="row" style="margin-top:12px;width:fit-content;height:32px;padding:0 12px 0 14px;background:var(--surface);border-radius:12px;gap:6px;color:var(--primary)">
      <span style="font-size:12px;font-weight:500">查看周报</span>
      %s
    </div>
  </div>

  <div class="card" style="margin-top:14px;padding:16px">
    <div style="font-size:11px;font-weight:500;letter-spacing:.06em;color:var(--tertiary)">这个月常写</div>
    <div class="row" style="margin-top:10px;gap:8px">
      <div class="chip">桂花 · 6</div>
      <div class="chip">读书 · 5</div>
      <div class="chip">散步 · 4</div>
      <div class="chip">平静 · 3</div>
    </div>
  </div>

  <div class="card" style="margin-top:14px;padding:0;overflow:hidden">
    %s
    %s
    %s
    %s
  </div>

  <div class="caption" style="margin-top:14px;text-align:center;color:var(--ghost)">随心记 v1.0.0 · 想记就记，不必完整</div>
</div>
%s""" % (
        icon("arrow", 12),
        setting("bell", "记录提醒", "每天 21:30"),
        setting("moon", "主题外观", "跟随系统"),
        setting("cloud", "数据与备份", ""),
        setting("info", "关于随心记", "v1.0.0"),
        nav("profile"),
    )


# ================================================================ 08 本周回顾弹窗
def screen_report():
    counts = [1, 2, 0, 1, 0, 3, 2]
    labels = ["四", "五", "六", "日", "一", "二", "三"]
    bars = ""
    for c, l in zip(counts, labels):
        h = 0 if c == 0 else 18 + int(c / 3.0 * 44)
        bars += ('<div class="bar-col"><div class="bar-fill" style="height:%dpx"></div>'
                 '<div class="bar-cap">%s</div></div>' % (h, l))

    def insight(text):
        return ('<div class="row" style="margin-bottom:7px;gap:8px">'
                '<span style="width:5px;height:5px;border-radius:9999px;background:var(--primary);flex:none"></span>'
                '<span style="font-size:12px;color:var(--secondary)">%s</span></div>' % text)

    overlay = """
<div class="scrim"></div>
<div class="dialog" style="padding:20px 20px 18px">
  <div class="row-b" style="align-items:flex-end">
    <div class="title20">本周回顾</div>
    <div style="font-size:11px;color:var(--tertiary)">10月16日 – 10月22日</div>
  </div>

  <div class="row" style="margin-top:14px;padding:14px 0;background:var(--surface_alt);border:1px solid var(--divider);border-radius:14px">
    <div style="flex:1;text-align:center">
      <div style="font-family:'Segoe UI',sans-serif;font-size:22px;font-weight:700">9</div>
      <div class="caption" style="margin-top:3px">条记录</div>
    </div>
    <div style="flex:1;text-align:center">
      <div style="font-family:'Segoe UI',sans-serif;font-size:22px;font-weight:700">5</div>
      <div class="caption" style="margin-top:3px">记录天数</div>
    </div>
    <div style="flex:1;text-align:center">
      <div style="font-family:'Segoe UI',sans-serif;font-size:22px;font-weight:700">1,240</div>
      <div class="caption" style="margin-top:3px">写下的字</div>
    </div>
  </div>

  <div class="row" style="margin-top:16px;height:74px;align-items:flex-end">%s</div>
  <div style="margin-top:6px;font-size:11px;color:var(--ghost)">点一下柱子，看那天写了多少</div>

  <div style="margin-top:12px">
    %s
  </div>

  <div style="margin-top:14px;border-radius:20px;padding:14px 14px 12px 16px;background:linear-gradient(135deg,var(--review_a),var(--review_b))">
    <div class="serif" style="font-size:14px;line-height:1.65;color:#FFFFFF">不用每天写，能回来就好。零星几句也算数，它们会替你记住这一周。</div>
    <div style="margin-top:8px;display:flex;justify-content:flex-end">
      <div class="chip" style="height:28px;display:flex;align-items:center;padding:0 10px;background:var(--primary_soft);color:var(--primary)">换一句</div>
    </div>
  </div>

  <div class="row" style="margin-top:16px;gap:10px">
    <div class="btn secondary" style="flex:1;height:44px;font-size:14px">看看这周</div>
    <div class="btn primary" style="flex:1;height:44px;font-size:14px">写一条</div>
  </div>
</div>""" % (bars,
             insight("这 7 天有 5 天写下东西，一共 9 条")
             + insight("星期四写得最多，3 条")
             + insight("常写的标签：散步、读书")
             + insight("平均每条 138 字"))

    return """
<div style="position:relative;height:864px;overflow:hidden">
  %s
  %s
</div>""" % (screen_profile().replace(nav("profile"), ""), overlay)


# ================================================================ 09 全屏看图
def screen_viewer():
    return """
<div style="position:relative;height:864px;background:#0D0C0B;overflow:hidden">
  %s

  <div class="row" style="position:absolute;left:14px;right:18px;top:12px">
    <div style="width:38px;height:38px;border-radius:9999px;background:rgba(255,255,255,.14);display:flex;align-items:center;justify-content:center;color:#fff">%s</div>
    <div class="spacer"></div>
    <div style="font-size:13px;font-weight:500;letter-spacing:.08em;color:rgba(255,255,255,.9)">1 / 3</div>
  </div>

  <div style="position:absolute;left:0;right:0;bottom:34px;display:flex;justify-content:center">
    <div style="background:rgba(0,0,0,.52);color:rgba(255,255,255,.86);font-size:11px;padding:7px 14px;border-radius:14px">双指缩放 · 双击放大 · 左右滑动切换</div>
  </div>
</div>""" % (photo_tall(), icon("close", 18, 2))


# ================================================================ 导出
VIEWER_CSS = """
body { background:#0D0C0B; }
.status { color:#EDE9E2; }
.bar, .bat::after, .bat::before { background:#EDE9E2; }
.bat { border-color:#EDE9E2; }
"""

SCREENS = [
    ("01-today", screen_today, LIGHT),
    ("02-editor", screen_editor, LIGHT),
    ("03-detail", screen_detail, LIGHT),
    ("04-timeline", screen_timeline, LIGHT),
    ("05-calendar", screen_calendar, LIGHT),
    ("06-search", screen_search, LIGHT),
    ("07-profile", screen_profile, LIGHT),
    ("08-weekly-report", screen_report, LIGHT),
    ("09-photo-viewer", screen_viewer, LIGHT, VIEWER_CSS),
    ("10-dark", screen_today, DARK),
]


def page(name, body, tokens, extra=""):
    return """<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<title>%(name)s</title>
<style>
:root {%(tokens)s}
%(css)s
%(extra)s
</style>
</head>
<body>
%(body)s
</body>
</html>""" % {
        "name": name,
        "tokens": tokens,
        "css": CSS.replace("__SANS__", SANS).replace("__SERIF__", SERIF),
        "body": status_bar() + body,
        "extra": extra,
    }


def main():
    if os.path.isdir(BUILD):
        shutil.rmtree(BUILD)
    os.makedirs(BUILD)
    for entry in SCREENS:
        name, fn, tokens = entry[0], entry[1], entry[2]
        extra = entry[3] if len(entry) > 3 else ""
        html = page(name, fn(), tokens, extra)
        with open(os.path.join(BUILD, name + ".html"), "w", encoding="utf-8") as f:
            f.write(html)
        print("write", name)


if __name__ == "__main__":
    main()
