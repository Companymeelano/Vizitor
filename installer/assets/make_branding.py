#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Vizitor branding generator  —  builds the icon and the NSIS wizard bitmaps
from the master icon (installer/assets/icon-v2-cutout.png).

Outputs (all inside installer/assets):
    vizitor.ico            16/24/32/48/64/128/256 px  (installer + app + shortcuts)
    vizitor.png            1024 px master (docs / Android / README)
    wizard-welcome.bmp     164x314  (MUI_WELCOMEFINISHPAGE_BITMAP)
    wizard-unwelcome.bmp   164x314  (MUI_UNWELCOMEFINISHPAGE_BITMAP)
    wizard-header.bmp      150x57   (MUI_HEADERIMAGE_BITMAP)

Run:  python3 installer/assets/make_branding.py
"""
import os

from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
MASTER = os.path.join(HERE, "icon-v2-cutout.png")

NAVY_TOP = (9, 14, 45)
NAVY_MID = (26, 42, 99)
NAVY_BOT = (8, 12, 38)
GOLD = (232, 199, 102)
GOLD_DEEP = (196, 154, 55)
INK = (233, 238, 255)
FONT_BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FONT_REG = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"


def gradient(size, top, mid, bottom):
    w, h = size
    img = Image.new("RGB", size)
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / float(max(1, h - 1))
        if t < 0.5:
            k = t / 0.5
            c = tuple(int(top[i] + (mid[i] - top[i]) * k) for i in range(3))
        else:
            k = (t - 0.5) / 0.5
            c = tuple(int(mid[i] + (bottom[i] - mid[i]) * k) for i in range(3))
        d.line([(0, y), (w, y)], fill=c)
    return img


def stripes(img, step=22, alpha=12, angle=32):
    w, h = img.size
    layer = Image.new("RGBA", (w * 2, h * 2), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    big = max(w, h) * 2
    for x in range(-big, 2 * big, step):
        d.line([(x, 0), (x + big, big)], fill=(255, 255, 255, alpha), width=6)
    layer = layer.rotate(angle, resample=Image.BICUBIC, expand=False)
    layer = layer.crop(((layer.size[0] - w) // 2, (layer.size[1] - h) // 2,
                        (layer.size[0] - w) // 2 + w, (layer.size[1] - h) // 2 + h))
    img.alpha_composite(layer)
    return img


def glow(img, center, radius, color=(120, 160, 255), alpha=60):
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    cx, cy = center
    d.ellipse([cx - radius, cy - radius, cx + radius, cy + radius], fill=color + (alpha,))
    layer = layer.filter(ImageFilter.GaussianBlur(radius * 0.55))
    img.alpha_composite(layer)
    return img


def paste_icon(base, icon, box_w, y, shadow=True):
    w, h = base.size
    ic = icon.copy()
    ic.thumbnail((box_w, box_w), Image.LANCZOS)
    x = (w - ic.size[0]) // 2
    if shadow:
        sh = Image.new("RGBA", base.size, (0, 0, 0, 0))
        s = Image.new("RGBA", ic.size, (0, 0, 0, 130))
        s.putalpha(ic.split()[3].point(lambda v: int(v * 0.5)))
        sh.paste(s, (x, y + 6), s)
        base.alpha_composite(sh.filter(ImageFilter.GaussianBlur(6)))
    base.alpha_composite(ic, (x, y))
    return base


def text_center(draw, cx, y, s, font, fill, spacing=0):
    if spacing:
        widths = [draw.textlength(ch, font=font) for ch in s]
        total = sum(widths) + spacing * (len(s) - 1)
        x = cx - total / 2.0
        for ch, cw in zip(s, widths):
            draw.text((x, y), ch, font=font, fill=fill)
            x += cw + spacing
        return total
    w = draw.textlength(s, font=font)
    draw.text((cx - w / 2.0, y), s, font=font, fill=fill)
    return w


def make_welcome(size=(164, 314), icon=None):
    img = gradient(size, NAVY_TOP, NAVY_MID, NAVY_BOT).convert("RGBA")
    img = stripes(img)
    img = glow(img, (size[0] // 2, 118), 74, (108, 150, 255), 70)

    img = paste_icon(img, icon, 108, 58)

    d = ImageDraw.Draw(img)
    f1 = ImageFont.truetype(FONT_BOLD, 15)
    f2 = ImageFont.truetype(FONT_REG, 9)
    text_center(d, size[0] // 2, 196, "VIZITOR", f1, GOLD + (255,), spacing=2)

    d.line([(size[0] // 2 - 32, 224), (size[0] // 2 + 32, 224)], fill=GOLD_DEEP + (230,), width=1)
    text_center(d, size[0] // 2, 236, "vizitor  |  atiran", f2, INK + (215,), spacing=0)

    # bottom / top accents
    d.rectangle([0, 0, size[0] - 1, 2], fill=GOLD + (255,))
    d.rectangle([0, size[1] - 3, size[0] - 1, size[1] - 1], fill=GOLD + (255,))
    d.rectangle([0, size[1] - 14, size[0] - 1, size[1] - 12], fill=(255, 255, 255, 26))
    # soft corner sheen
    sheen = Image.new("RGBA", size, (0, 0, 0, 0))
    ImageDraw.Draw(sheen).polygon([(0, 0), (size[0], 0), (0, int(size[1] * 0.42))],
                                 fill=(255, 255, 255, 12))
    img.alpha_composite(sheen)
    return img.convert("RGB")


def make_header(size=(150, 57), icon=None):
    img = gradient(size, (12, 20, 58), (30, 48, 108), (10, 16, 46)).convert("RGBA")
    img = stripes(img, step=16, alpha=10, angle=32)
    img = glow(img, (size[0] // 2, size[1] // 2), 40, (110, 150, 255), 55)
    ic = icon.copy()
    ic.thumbnail((40, 40), Image.LANCZOS)
    img.alpha_composite(ic, ((size[0] - ic.size[0]) // 2, (size[1] - ic.size[1]) // 2))
    d = ImageDraw.Draw(img)
    d.rectangle([0, size[1] - 2, size[0] - 1, size[1] - 1], fill=GOLD + (255,))
    return img.convert("RGB")


def main():
    icon = Image.open(MASTER).convert("RGBA")
    icon = icon.resize((1024, 1024), Image.LANCZOS)

    # --- master png ---------------------------------------------------------
    icon.save(os.path.join(HERE, "vizitor.png"))
    icon.resize((512, 512), Image.LANCZOS).save(os.path.join(HERE, "vizitor-icon.png"))

    # --- multi-size ico -----------------------------------------------------
    sizes = [256, 128, 64, 48, 32, 24, 16]
    frames = []
    for s in sizes:
        im = icon.resize((s, s), Image.LANCZOS)
        if s <= 48:                       # crisper small sizes: a touch of contrast
            r, g, b, a = im.split()
            a = a.point(lambda v: int(min(255, v * 1.08)))
            im = Image.merge("RGBA", (r, g, b, a))
        frames.append(im)
    frames[0].save(os.path.join(HERE, "vizitor.ico"), format="ICO",
                   sizes=[(s, s) for s in sizes], append_images=frames[1:])

    # --- wizard bitmaps -----------------------------------------------------
    w = make_welcome(icon=icon)
    w.save(os.path.join(HERE, "wizard-welcome.bmp"), format="BMP")
    w.save(os.path.join(HERE, "wizard-unwelcome.bmp"), format="BMP")
    make_header(icon=icon).save(os.path.join(HERE, "wizard-header.bmp"), format="BMP")

    # --- previews (2x, for eyeballing) --------------------------------------
    w.resize((328, 628), Image.NEAREST).save("/tmp/wizard-welcome.png")
    make_header(icon=icon).resize((300, 114), Image.NEAREST).save("/tmp/wizard-header.png")

    for f in ("vizitor.ico", "vizitor.png", "vizitor-icon.png",
              "wizard-welcome.bmp", "wizard-unwelcome.bmp", "wizard-header.bmp"):
        p = os.path.join(HERE, f)
        print("%-24s %8d bytes" % (f, os.path.getsize(p)))
    print("icon sizes in ico:", sizes)


if __name__ == "__main__":
    main()
