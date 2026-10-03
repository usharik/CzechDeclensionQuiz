#!/usr/bin/env python3
"""Builds the Google Play phone screenshots for Czech Declension Quiz.

Input:  store/raw/<locale>/NN_<name>.png (1080x2400 emulator captures, see capture_screens.py)
        store/raw/<locale>/NN_<name>.ad.json (ad banner bounds, painted over with the screen background)
Output: fastlane/metadata/android/<locale>/images/phoneScreenshots/NN.png, 1080x1920 24-bit PNG

Each slide: brand-coloured gradient backdrop with large faded Czech letters, a two-line headline with one
highlighted phrase ([[...]]), a subline, the capture in a phone frame bleeding off the bottom edge, and
floating "chips" that echo the app's draggable word chips.

Requirements: Pillow; store/fonts/Roboto.ttf (variable Roboto pulled from the emulator's /system/fonts).
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent
RAW = ROOT / "raw"
FONT = ROOT / "fonts" / "Roboto.ttf"
METADATA = ROOT.parent / "fastlane" / "metadata" / "android"

W, H = 1080, 1920
DEVICE_W, DEVICE_TOP, DEVICE_RADIUS, BEZEL = 800, 480, 96, 18
HIGHLIGHT = (255, 213, 79)  # amber 300

# Gradients taken from the hub's quiz-mode buttons: indigo/purple (full table), teal (one case), orange (mistakes).
PALETTES = {
    "indigo": ((94, 53, 177), (48, 63, 159)),
    "teal": ((0, 137, 123), (0, 96, 100)),
    "orange": ((245, 124, 0), (216, 67, 21)),
    "deep": ((49, 27, 146), (26, 35, 126)),
}

# (raw capture, palette, chips: [(text, side, y, angle)]) - chips stick out of the phone's left/right edge at canvas y,
# placed over empty parts of the screen. A trailing "!" makes the chip amber.
SLIDES = [
    ("01_hub", "deep", []),
    ("02_noun_table", "indigo", [("koho? čeho?", "L", 1110, -5), ("kým? čím?!", "R", 1800, 5)]),
    ("03_adjective_table", "teal", [("mladý muž", "L", 1110, -5), ("mladého muže!", "R", 1800, 5)]),
    ("04_verb_table", "orange", [("já dělám", "L", 1110, -5), ("dělat → udělat!", "R", 1800, 5)]),
    ("05_phrase_single", "indigo", [("bez ___", "L", 800, -6), ("o ___!", "R", 1830, 5)]),
    ("06_handbook", "teal", [("mladý → mladí!", "R", 935, 5)]),
]

CAPTIONS = {
    "en-US": [
        ("Learn Czech grammar\n[[every day]]", "Daily goal, streak and 2,000 words"),
        ("Master all\n[[7 cases]]", "Drag every form into the declension table"),
        ("Adjectives that\n[[agree]]", "Always practised together with a noun"),
        ("[[Conjugate]]\nCzech verbs", "Present, past, imperative and aspect pairs"),
        ("One case\n[[at a time]]", "Pick the right form after bez, ke, vidím, o, s"),
        ("Every pattern\n[[in one handbook]]", "Short grammar tips for every type"),
    ],
    "ru-RU": [
        ("Чешская грамматика\n[[каждый день]]", "Дневная цель, серия дней и 2000 слов"),
        ("Все [[7 падежей]]\nв одной таблице", "Перетащите каждую форму на своё место"),
        ("Прилагательные\n[[в согласовании]]", "Всегда в паре с существительным"),
        ("[[Спряжение]]\nчешских глаголов", "Настоящее, прошедшее, повелительное"),
        ("По одному\n[[падежу за раз]]", "Выберите форму после bez, ke, vidím, o, s"),
        ("Все образцы\n[[в справочнике]]", "Короткие подсказки по грамматике"),
    ],
    "cs-CZ": [
        ("Česká gramatika\n[[každý den]]", "Denní cíl, série dní a 2000 slov"),
        ("Všech [[7 pádů]]\nv jedné tabulce", "Přetáhněte každý tvar na správné místo"),
        ("Přídavná jména\n[[ve shodě]]", "Vždy spolu s podstatným jménem"),
        ("[[Časování]]\nčeských sloves", "Přítomný čas, minulý čas, rozkazovací způsob"),
        ("Jeden pád\n[[po druhém]]", "Vyberte tvar po bez, ke, vidím, o, s"),
        ("Všechny vzory\n[[v příručce]]", "Krátké gramatické tipy ke každému typu"),
    ],
}
BACKDROP_LETTERS = ["č", "ř", "ů", "é", "š", "ý"]


def font(size: int, weight: int) -> ImageFont.FreeTypeFont:
    f = ImageFont.truetype(str(FONT), size)
    f.set_variation_by_axes([weight, 100, 0])
    return f


def gradient(size, top, bottom) -> Image.Image:
    """Diagonal gradient (top-left → bottom-right)."""
    w, h = size
    base = Image.linear_gradient("L").rotate(-35, expand=True).resize((w, h))
    a, b = Image.new("RGB", size, top), Image.new("RGB", size, bottom)
    return Image.composite(b, a, base)


def backdrop(palette: str, letter: str) -> Image.Image:
    top, bottom = PALETTES[palette]
    img = gradient((W, H), top, bottom).convert("RGBA")
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.ellipse((-260, 980, 620, 1860), fill=(255, 255, 255, 26))
    d.ellipse((700, 380, 1380, 1060), fill=(255, 255, 255, 18))
    layer = layer.filter(ImageFilter.GaussianBlur(90))
    img.alpha_composite(layer)
    glyphs = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(glyphs).text((W - 40, 1500), letter, font=font(900, 900), fill=(255, 255, 255, 22), anchor="rm")
    img.alpha_composite(glyphs)
    return img


def parse_line(line: str):
    """'Master all [[7 cases]]' -> [('Master all ', False), ('7 cases', True)]"""
    parts = re.split(r"(\[\[.*?\]\])", line)
    return [(p[2:-2], True) if p.startswith("[[") else (p, False) for p in parts if p]


def draw_caption(img: Image.Image, headline: str, subline: str):
    d = ImageDraw.Draw(img)
    lines = headline.split("\n")
    size = 96
    while size > 56:
        head = font(size, 900)
        if all(d.textlength("".join(t for t, _ in parse_line(l)), font=head) <= W - 120 for l in lines):
            break
        size -= 4
    y = 110
    pad = round(size * 0.16)
    for line in lines:
        segs = parse_line(line)
        widths = [d.textlength(t, font=head) + (2 * pad if hl else 0) for t, hl in segs]
        x = (W - sum(widths)) / 2
        for (text, hl), tw in zip(segs, widths):
            if hl:
                # solid amber pill behind the highlighted words, dark text on top
                pill = Image.new("RGBA", img.size, (0, 0, 0, 0))
                ImageDraw.Draw(pill).rounded_rectangle(
                    (x, y + size * 0.08, x + tw, y + size * 1.16), round(size * 0.24), fill=HIGHLIGHT + (255,))
                img.alpha_composite(pill.rotate(-1.5, center=(x + tw / 2, y + size * 0.6), resample=Image.BICUBIC))
                d = ImageDraw.Draw(img)
                d.text((x + pad, y), text, font=head, fill=(40, 30, 90))
            else:
                d.text((x, y), text, font=head, fill=(255, 255, 255))
            x += tw
        y += size * 1.2
    sub = font(42, 400)
    sw = d.textlength(subline, font=sub)
    if sw > W - 100:
        sub = font(int(42 * (W - 100) / sw), 400)
        sw = d.textlength(subline, font=sub)
    d.text(((W - sw) / 2, y + 10), subline, font=sub, fill=(255, 255, 255, 225))


def load_shot(raw: Path) -> Image.Image:
    shot = Image.open(raw).convert("RGB")
    meta = raw.with_suffix(".ad.json")
    box = json.loads(meta.read_text())["ad"] if meta.exists() else None
    if box:
        x1, y1, x2, y2 = box
        bg = shot.getpixel((8, min(y1 + 10, shot.height - 1)))
        ImageDraw.Draw(shot).rectangle((0, y1 - 4, shot.width, y2 + 4), fill=bg)
    return shot


def phone(shot: Image.Image) -> Image.Image:
    inner_w = DEVICE_W - 2 * BEZEL
    shot = shot.resize((inner_w, round(shot.height * inner_w / shot.width)), Image.LANCZOS)
    w, h = DEVICE_W, shot.height + 2 * BEZEL
    dev = Image.new("RGBA", (w + 12, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(dev)
    # side buttons
    d.rounded_rectangle((w - 4, 300, w + 8, 420), 6, fill=(30, 30, 36, 255))
    d.rounded_rectangle((w - 4, 470, w + 8, 650), 6, fill=(30, 30, 36, 255))
    # body: dark frame with a subtle lighter rim
    d.rounded_rectangle((0, 0, w - 1, h - 1), DEVICE_RADIUS, fill=(58, 58, 66, 255))
    d.rounded_rectangle((3, 3, w - 4, h - 4), DEVICE_RADIUS - 3, fill=(16, 16, 20, 255))
    mask = Image.new("L", shot.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, shot.width - 1, shot.height - 1), DEVICE_RADIUS - BEZEL, fill=255)
    dev.paste(shot, (BEZEL, BEZEL), mask)
    # punch-hole camera
    cx = w // 2
    d.ellipse((cx - 13, BEZEL + 24, cx + 13, BEZEL + 50), fill=(10, 10, 12, 255))
    return dev


ARROW_FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"  # Roboto has no U+2192


def chip(text: str, angle: float, accent: bool) -> Image.Image:
    f, fa = font(50, 600), ImageFont.truetype(ARROW_FONT, 44)
    parts = [(p, fa if p == "→" else f) for p in re.split(r"(→)", text) if p]
    measure = ImageDraw.Draw(Image.new("RGB", (1, 1)))
    tw = sum(measure.textlength(p, font=pf) for p, pf in parts)
    pad_x, ch = 40, 104
    cw = int(tw + 2 * pad_x)
    m = 50  # margin for the shadow
    img = Image.new("RGBA", (cw + 2 * m, ch + 2 * m), (0, 0, 0, 0))
    sh = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ImageDraw.Draw(sh).rounded_rectangle((m, m + 14, m + cw, m + ch + 14), 30, fill=(0, 0, 0, 110))
    img.alpha_composite(sh.filter(ImageFilter.GaussianBlur(16)))
    d = ImageDraw.Draw(img)
    fill = HIGHLIGHT if accent else (255, 255, 255)
    d.rounded_rectangle((m, m, m + cw, m + ch), 30, fill=fill + (255,), outline=(197, 202, 233, 255) if not accent else None, width=3)
    x = m + pad_x
    for p, pf in parts:
        d.text((x, m + ch / 2), p, font=pf, fill=(33, 33, 50), anchor="lm")
        x += d.textlength(p, font=pf)
    return img.rotate(angle, resample=Image.BICUBIC, expand=True)


def build(index: int, raw: Path, palette: str, chips, caption) -> Image.Image:
    canvas = backdrop(palette, BACKDROP_LETTERS[index % len(BACKDROP_LETTERS)])
    draw_caption(canvas, *caption)
    dev = phone(load_shot(raw))
    x = (W - DEVICE_W) // 2
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle((x + 10, DEVICE_TOP + 40, x + DEVICE_W - 10, DEVICE_TOP + dev.height + 40),
                                             DEVICE_RADIUS, fill=(0, 0, 0, 130))
    canvas.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(45)))
    canvas.alpha_composite(dev, (x, DEVICE_TOP))
    for text, side, cy, angle in chips:
        accent = text.endswith("!")  # a trailing "!" marks the amber accent chip
        c = chip(text.rstrip("!"), angle, accent)
        px = -20 if side == "L" else W - c.width + 20
        canvas.alpha_composite(c, (px, int(cy - c.height / 2)))
    return canvas.convert("RGB")


def main() -> int:
    locales = sys.argv[1:] or list(CAPTIONS)
    for locale in locales:
        out = METADATA / locale / "images" / "phoneScreenshots"
        out.mkdir(parents=True, exist_ok=True)
        for stale in out.glob("*.png"):
            stale.unlink()
        for i, (name, palette, chips) in enumerate(SLIDES):
            raw = RAW / locale / f"{name}.png"
            path = out / f"{i + 1:02d}.png"
            build(i, raw, palette, chips, CAPTIONS[locale][i]).save(path, optimize=True)
            print(path.relative_to(ROOT.parent), f"{path.stat().st_size // 1024} KB")
    return 0


if __name__ == "__main__":
    sys.exit(main())
