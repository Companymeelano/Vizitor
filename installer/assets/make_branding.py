#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Vizitor setup branding generator  (installer/assets)

Builds every bitmap the NSIS wizard needs, from one place, so the look stays
consistent and can be re-generated at any time:

    wizard-welcome.bmp   164 x 314   MUI welcome / finish (side panel)
    wizard-header.bmp    150 x  57   MUI inner-page header
    wizard-unwelcome.bmp 164 x 314   uninstaller side panel

Run:
    python make_branding.py

Requires: pillow  (pip install pillow)
"""
from PIL import Image, ImageDraw, ImageFont
import os

HERE = os.path.dirname(os.path.abspath(__file__))

NAVY_TOP = (10, 23, 48)
NAVY_BOT = (27, 42, 99)
GOLD_1 = (247, 205, 96)
GOLD_2 = (222, 165, 42)
TEAL = (34, 205, 194)
INK = (233, 238, 250)
PAPER = (247, 249, 252)

FONT_BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FONT_REG = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"


def vgradient(w, h, top, bottom):
    img = Image.new("RGB", (w, h))
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / max(h - 1, 1)
        d.line([(0, y), (w, y)], fill=(
            int(top[0] + (bottom[0] - top[0]) * t),
            int(top[1] + (bottom[1] - top[1]) * t),
            int(top[2] + (bottom[2] - top[2]) * t)))
    return img


def draw_v(img, cx, cy, size, color_hi=GOLD_1, color_lo=GOLD_2, pin=True):
    """Draw the Vizitor 'V' mark: two thick strokes + a small pin dot."""
    d = ImageDraw.Draw(img)
    s = size
    stroke = max(2, int(s * 0.20))
    left_top = (cx - s / 2, cy - s / 2)
    right_top = (cx + s / 2, cy - s / 2)
    bottom = (cx, cy + s / 2)
    # left stroke
    d.line([left_top, bottom], fill=color_hi, width=stroke)
    # right stroke (slightly darker, gives depth)
    d.line([right_top, bottom], fill=color_lo, width=stroke)
    # flat tops
    for p in (left_top, right_top):
        d.line([(p[0] - stroke / 2, p[1]), (p[0] + stroke / 2, p[1])],
               fill=color_hi, width=max(1, stroke // 3))
    if pin:
        r = max(2, int(s * 0.075))
        px, py = right_top[0], right_top[1] - int(s * 0.13)
        d.ellipse([px - r, py - r, px + r, py + r], fill=TEAL)
    return img


def vtext(text, font, fill, spacing=0):
    """Render rotated (90 deg CCW) text; returns an RGBA image."""
    probe = Image.new("RGBA", (10, 10))
    pd = ImageDraw.Draw(probe)
    widths, total = [], 0
    for ch in text:
        w = pd.textlength(ch, font=font)
        widths.append(w)
        total += w
    total += spacing * (len(text) - 1)
    asc, desc = font.getmetrics()
    strip = Image.new("RGBA", (int(total) + 8, asc + desc + 8), (0, 0, 0, 0))
    sd = ImageDraw.Draw(strip)
    x = 4
    for ch, w in zip(text, widths):
        sd.text((x, 4), ch, font=font, fill=fill)
        x += w + spacing
    return strip.rotate(90, expand=True)


def welcome(path):
    W, H = 164, 314
    img = vgradient(W, H, NAVY_TOP, NAVY_BOT)
    d = ImageDraw.Draw(img, "RGBA")
    # soft diagonal light streaks
    for i in range(4):
        off = -40 + i * 46
        d.polygon([(off, H), (off + 26, H), (off + 26 + 90, 0), (off + 90, 0)],
                  fill=(255, 255, 255, 9))
    # top gold hairline
    d.rectangle([0, 0, W, 3], fill=GOLD_2)
    # V mark
    draw_v(img, W / 2, 104, 74)
    # rising bars under the mark (growth)
    bx, by, bw = W / 2 - 40, 168, 13
    for i, bh in enumerate((14, 22, 32)):
        d.rectangle([bx + i * (bw + 7), by + (32 - bh), bx + i * (bw + 7) + bw, by + 32],
                    fill=GOLD_1 if i < 2 else GOLD_2)
    # wordmark (rotated, latin - keeps the bitmap font-safe)
    t = vtext("VIZITOR", ImageFont.truetype(FONT_REG, 17), INK + (235,), spacing=6)
    img.paste(t, (int(W - t.width - 16), int((H - t.height) / 2) - 30), t)
    # footer band + build stamp
    d.rectangle([0, H - 34, W, H - 31], fill=GOLD_2)
    f = ImageFont.truetype(FONT_REG, 10)
    d.text((12, H - 24), "vizitor  |  atiran", font=f, fill=(196, 205, 228))
    img.convert("RGB").save(path)


def header(path):
    W, H = 150, 57
    img = Image.new("RGB", (W, H), PAPER)
    d = ImageDraw.Draw(img)
    for y in range(H):
        t = y / (H - 1)
        d.line([(0, y), (W, y)], fill=(int(247 - 6 * t), int(249 - 4 * t), int(252 - 2 * t)))
    d.rectangle([0, H - 3, W, H], fill=(NAVY_BOT[0], NAVY_BOT[1], NAVY_BOT[2]))
    draw_v(img, 34, 27, 34, pin=True)
    d.line([(62, 16), (134, 16)], fill=(214, 221, 236), width=1)
    d.line([(62, 24), (118, 24)], fill=(224, 230, 242), width=1)
    img.save(path)


def main():
    welcome(os.path.join(HERE, "wizard-welcome.bmp"))
    welcome(os.path.join(HERE, "wizard-unwelcome.bmp"))
    header(os.path.join(HERE, "wizard-header.bmp"))
    for f in ("wizard-welcome.bmp", "wizard-unwelcome.bmp", "wizard-header.bmp"):
        p = os.path.join(HERE, f)
        print("%-24s %8d B" % (f, os.path.getsize(p)))


if __name__ == "__main__":
    main()
