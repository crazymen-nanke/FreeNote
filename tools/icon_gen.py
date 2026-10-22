# -*- coding: utf-8 -*-
"""
随心记 · 应用图标生成器。

图形语言：茶绿底 + 纸白便签 + 折角 + 三行手记 + 陶土色落款点。
同一套几何既用于 Android 自适应图标（矢量），也用于 API 24/25 需要的位图。

用法：python tools/icon_gen.py
"""
import os
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")

# 品牌色
GREEN_TOP = (94, 124, 82)
GREEN_BOTTOM = (62, 87, 55)
PAPER = (248, 245, 240)
PAPER_FOLD = (226, 217, 201)
RULE = (169, 189, 160)
SEAL = (216, 149, 117)

S = 8          # 超采样倍数
BASE = 108     # 设计基准边长，与矢量 viewport 一致


def vertical_gradient(size, top, bottom):
    img = Image.new("RGB", (1, size), top)
    px = img.load()
    for y in range(size):
        t = y / max(size - 1, 1)
        px[0, y] = (
            round(top[0] + (bottom[0] - top[0]) * t),
            round(top[1] + (bottom[1] - top[1]) * t),
            round(top[2] + (bottom[2] - top[2]) * t),
        )
    return img.resize((size, size), Image.BILINEAR)


def draw_foreground(layer, offset_x, offset_y, scale):
    """在 RGBA 图层上绘制便签，坐标以 108 视口为基准。"""
    d = ImageDraw.Draw(layer)

    def box(x0, y0, x1, y1):
        return (
            offset_x + x0 * scale,
            offset_y + y0 * scale,
            offset_x + x1 * scale,
            offset_y + y1 * scale,
        )

    def width(v):
        return max(1, round(v * scale))

    # 便签主体
    d.rounded_rectangle(box(35, 29, 73, 79), radius=width(6), fill=PAPER)

    # 右上折角
    d.polygon(
        [
            (offset_x + 64 * scale, offset_y + 29 * scale),
            (offset_x + 73 * scale, offset_y + 38 * scale),
            (offset_x + 64 * scale, offset_y + 38 * scale),
        ],
        fill=PAPER_FOLD,
    )

    # 三行字迹
    for y, x1 in ((45, 64), (53, 64), (61, 57)):
        d.line(
            [(offset_x + 43 * scale, offset_y + y * scale),
             (offset_x + x1 * scale, offset_y + y * scale)],
            fill=RULE,
            width=width(2.8),
        )
        r = width(1.4)
        for x in (43, x1):
            cy = offset_y + y * scale
            cx = offset_x + x * scale
            d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=RULE)

    # 落款点
    r = width(4.6)
    cx = offset_x + 67.5 * scale
    cy = offset_y + 71 * scale
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=SEAL)


def adaptive_foreground():
    """透明前景：供预览与核对，实际打包使用矢量。"""
    size = BASE * S
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw_foreground(layer, 0, 0, S)
    return layer


def legacy_icon(round_icon=False, size=1024):
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    bg = vertical_gradient(size, GREEN_TOP, GREEN_BOTTOM).convert("RGBA")
    mask = Image.new("L", (size, size), 0)
    md = ImageDraw.Draw(mask)
    if round_icon:
        pad = round(size * 0.02)
        md.ellipse([pad, pad, size - pad, size - pad], fill=255)
    else:
        md.rounded_rectangle([0, 0, size - 1, size - 1], radius=round(size * 0.24), fill=255)
    canvas.paste(bg, (0, 0), mask)

    # 传统图标没有安全区裁切，便签放大一点，视觉重量与自适应版一致
    scale = (size / BASE) * 1.18
    draw_foreground(canvas, (size - BASE * scale) / 2, (size - BASE * scale) / 2, scale)
    return canvas


def save(img, path, px):
    out = img.resize((px, px), Image.LANCZOS)
    out.save(path, "PNG", optimize=True)
    print("  " + os.path.relpath(path, ROOT))


def main():
    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }
    square = legacy_icon(False)
    round_ = legacy_icon(True)

    for folder, px in densities.items():
        target = os.path.join(RES, folder)
        os.makedirs(target, exist_ok=True)
        print(folder)
        save(square, os.path.join(target, "ic_launcher.png"), px)
        save(round_, os.path.join(target, "ic_launcher_round.png"), px)

    # 仅用于设计核对，不参与打包
    preview_dir = os.path.join(ROOT, "tools", "preview")
    os.makedirs(preview_dir, exist_ok=True)
    legacy_icon(False, 512).save(os.path.join(preview_dir, "icon_square.png"))
    legacy_icon(True, 512).save(os.path.join(preview_dir, "icon_round.png"))
    adaptive_foreground().resize((512, 512), Image.LANCZOS).save(
        os.path.join(preview_dir, "icon_foreground.png"))
    print("preview -> tools/preview")


if __name__ == "__main__":
    main()
