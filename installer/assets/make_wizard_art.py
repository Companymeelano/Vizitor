#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Vizitor wizard artwork generator — سه‌بعدی و براق
=================================================
Builds the NSIS wizard bitmaps used by MUI:

    wizard-header.bmp    150x57   (MUI_HEADERIMAGE_BITMAP — top of every page)
    wizard-welcome.bmp   164x314  (MUI_WELCOMEFINISHPAGE_BITMAP — welcome/finish)
    wizard-unwelcome.bmp 164x314  (uninstaller counterpart)

Palette matches installer/vizitor.nsi:
    navy 0x0B2138 / 0x123A5E / 0x1B5586 , gold 0xD9A23C , ice 0xEAF3FB

Run:  python3 installer/assets/make_wizard_art.py
"""
import os

from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))

NAVY1 = (11, 33, 56)
NAVY2 = (18, 58, 94)
NAVY3 = (27, 85, 134)
GOLD = (217, 162, 60)
GOLD_D = (150, 106, 26)
ICE = (234, 243, 251)
WHITE = (255, 255, 255)

FONT_DIR = "/usr/share/fonts/truetype/dejavu"


def font(name, size):
    p = os.path.join(FONT_DIR, name)
    return ImageFont.truetype(p, size) if os.path.exists(p) else ImageFont.load_default()


def vgrad(size, top, bottom):
    """vertical gradient"""
    w, h = size
    img = Image.new("RGB", size)
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / max(1, h - 1)
        d.line([(0, y), (w, y)], fill=tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3)))
    return img


def hgrad(size, left, right):
    w, h = size
    img = Image.new("RGB", size)
    d = ImageDraw.Draw(img)
    for x in range(w):
        t = x / max(1, w - 1)
        d.line([(x, 0), (x, h)], fill=tuple(int(left[i] + (right[i] - left[i]) * t) for i in range(3)))
    return img


def gloss(img, angle_deg=20, bands=((0.05, 0.30, 70), (0.42, 0.55, 38)), blur=6):
    """translucent light streaks -> 3D gloss"""
    w, h = img.size
    layer = Image.new("L", (w * 2, h * 2), 0)
    d = ImageDraw.Draw(layer)
    for (a, b, alpha) in bands:
        d.line([(int(w * 2 * a), 0), (int((w * 2 * b) - h * 0.9), h * 2)], fill=alpha, width=int(h * 0.9))
    layer = layer.rotate(angle_deg, resample=Image.BICUBIC)
    layer = layer.crop((w // 2, h // 2, w // 2 + w, h // 2 + h)).filter(ImageFilter.GaussianBlur(blur))
    white = Image.new("RGB", (w, h), WHITE)
    return Image.composite(white, img, layer.point(lambda v: min(255, v)))


def bevel_border(img, outer=ICE, inner=(90, 140, 190), width=1, inset=0):
    d = ImageDraw.Draw(img)
    w, h = img.size
    d.rectangle([inset, inset, w - 1 - inset, h - 1 - inset], outline=outer, width=width)
    d.rectangle([inset + width, inset + width, w - 1 - inset - width, h - 1 - inset - width],
                outline=inner, width=1)


def emblem(size=64, ring=GOLD, face=NAVY1, letter="V", glow=True):
    """glossy 3D round emblem with a gold ring"""
    s = size * 4  # supersample
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    if glow:
        for i in range(14, 0, -1):
            a = int(26 * (i / 14) ** 2)
            d.ellipse([-i * 2, -i * 2, s + i * 2, s + i * 2], fill=(255, 220, 150, a))
    # outer gold ring (gradient-ish: two arcs)
    d.ellipse([0, 0, s - 1, s - 1], fill=GOLD)
    d.ellipse([0, 0, s - 1, s - 1], outline=(255, 235, 180, 220), width=max(1, s // 42))
    d.ellipse([s * 0.06, s * 0.06, s * 0.94, s * 0.94], fill=GOLD_D)
    # dark inner disc
    d.ellipse([s * 0.13, s * 0.13, s * 0.87, s * 0.87], fill=face)
    d.ellipse([s * 0.13, s * 0.13, s * 0.87, s * 0.87], outline=(120, 190, 240, 200), width=max(1, s // 40))
    # glossy highlight on the top-left of the disc
    hl = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    hd = ImageDraw.Draw(hl)
    hd.ellipse([s * 0.18, s * 0.10, s * 0.60, s * 0.44], fill=(255, 255, 255, 62))
    img = Image.alpha_composite(img, hl.filter(ImageFilter.GaussianBlur(s // 28)))
    # the letter
    f = font("DejaVuSans-Bold.ttf", int(s * 0.62))
    d = ImageDraw.Draw(img)
    bb = d.textbbox((0, 0), letter, font=f)
    tx, ty = (s - (bb[2] - bb[0])) / 2 - bb[0], (s - (bb[3] - bb[1])) / 2 - bb[1]
    d.text((tx + s * 0.012, ty + s * 0.016), letter, font=f, fill=(0, 0, 0, 120))       # shadow
    d.text((tx, ty), letter, font=f, fill=(255, 250, 235, 255))                          # face
    return img.resize((size, size), Image.LANCZOS)


def make_header(path):
    w, h = 150, 57
    img = hgrad((w, h), NAVY1, NAVY3)
    img = gloss(img, angle_deg=18, bands=((0.02, 0.34, 60),), blur=5)
    d = ImageDraw.Draw(img)
    d.line([(0, h - 1), (w, h - 1)], fill=GOLD)
    d.line([(0, h - 2), (w, h - 2)], fill=GOLD_D)
    d.line([(0, 0), (w, 0)], fill=(150, 200, 240))
    em = emblem(40)
    img.paste(em, (7, (h - 40) // 2), em)
    f1 = font("DejaVuSans-Bold.ttf", 15)
    f2 = font("DejaVuSans.ttf", 7)
    d.text((55, 12), "VIZITOR", font=f1, fill=WHITE)
    d.text((56, 30), "SMART VISITOR SYSTEM", font=f2, fill=(196, 220, 244))
    d.text((56, 40), "Meelano Studio Design", font=f2, fill=GOLD)
    img.save(path, "BMP")
    return img


def make_welcome(path):
    w, h = 164, 314
    img = vgrad((w, h), NAVY1, NAVY3)
    img = gloss(img, angle_deg=24, bands=((0.10, 0.40, 46), (0.55, 0.72, 30)), blur=9)
    d = ImageDraw.Draw(img)
    # top emblem plate
    plate = Image.new("RGBA", (w, 118), (0, 0, 0, 0))
    pd = ImageDraw.Draw(plate)
    pd.rounded_rectangle([10, 12, w - 11, 104], radius=14, fill=(8, 24, 42, 210),
                         outline=(120, 175, 225, 190))
    pd.ellipse([8, 8, w - 8, 108], fill=(0, 0, 0, 60))
    img_rgba = img.convert("RGBA")
    img_rgba.alpha_composite(plate, (0, 0))
    em = emblem(74)
    img_rgba.paste(em, ((w - 74) // 2, 22), em)
    img = img_rgba.convert("RGB")
    d = ImageDraw.Draw(img)
    # wordmark
    fv = font("DejaVuSans-Bold.ttf", 24)
    fs = font("DejaVuSans.ttf", 8)
    fb = font("DejaVuSans-Bold.ttf", 10)
    txt = "VIZITOR"
    bb = d.textbbox((0, 0), txt, font=fv)
    d.text(((w - (bb[2] - bb[0])) / 2 - bb[0], 128), txt, font=fv, fill=(0, 0, 0))
    d.text(((w - (bb[2] - bb[0])) / 2 - bb[0], 126), txt, font=fv, fill=WHITE)
    d.line([(26, 168), (w - 27, 168)], fill=GOLD, width=2)
    d.line([(40, 172), (w - 41, 172)], fill=(140, 190, 230), width=1)
    for i, (t, f, c) in enumerate([
            ("SMART VISITOR SYSTEM", fs, (198, 222, 246)),
            ("Direct SQL Server connection", fs, (170, 200, 228)),
            ("Port 1433  •  No IIS  •  No API", fs, (170, 200, 228))]):
        tb = d.textbbox((0, 0), t, font=f)
        d.text(((w - (tb[2] - tb[0])) / 2 - tb[0], 182 + i * 14), t, font=f, fill=c)
    # gold footer band
    d.rectangle([0, h - 74, w, h - 72], fill=GOLD)
    d.rectangle([0, h - 72, w, h], fill=(8, 24, 42))
    ft = "Meelano Studio Design"
    tb = d.textbbox((0, 0), ft, font=fb)
    d.text(((w - (tb[2] - tb[0])) / 2 - tb[0], h - 60), ft, font=fb, fill=GOLD)
    f2 = font("DejaVuSans.ttf", 8)
    for i, t in enumerate(["Milad Yaghoobi", "design & development", "v1.3"]):
        tb = d.textbbox((0, 0), t, font=f2)
        d.text(((w - (tb[2] - tb[0])) / 2 - tb[0], h - 44 + i * 12), t, font=f2, fill=(186, 210, 234))
    bevel_border(img, outer=(150, 200, 240), inner=(60, 105, 150), width=1)
    img.save(path, "BMP")
    return img


if __name__ == "__main__":
    make_header(os.path.join(HERE, "wizard-header.bmp")).save("/tmp/hdr_preview.png")
    w = make_welcome(os.path.join(HERE, "wizard-welcome.bmp"))
    w.save(os.path.join(HERE, "wizard-unwelcome.bmp"), "BMP")
    w.save("/tmp/wel_preview.png")
    print("written: wizard-header.bmp, wizard-welcome.bmp, wizard-unwelcome.bmp")
