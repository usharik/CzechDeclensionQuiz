#!/usr/bin/env python3
"""Captures the raw Google Play screenshots from a running emulator (1080x2400, debug build installed).

Usage:
  store/capture_screens.py <locale-dir> <language as listed in the app's language picker> [shot ...]
  e.g. store/capture_screens.py ru-RU Русский
       store/capture_screens.py cs-CZ Čeština hub

Shots: hub noun adjective verb phrase handbook (default: all). Output: store/raw/<locale-dir>/NN_<name>.png
plus NN_<name>.ad.json with the ad banner bounds, which build_assets.py paints over.

The quiz tables are filled with correct answers taken from the corpus in database/src/main/assets, so the
error counters stay at 0. Navigation relies on the Compose test tags (exposed as resource-ids).
"""
from __future__ import annotations

import json
import re
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent
ASSETS = ROOT.parent / "database" / "src" / "main" / "assets"
GENDER = {"m. živ.": 0, "m. neživ.": 1, "ž.": 2, "s.": 3}


def adb(cmd: str) -> bytes:
    return subprocess.run(f"adb {cmd}", shell=True, capture_output=True).stdout


def dump() -> list[dict]:
    xml = adb("exec-out uiautomator dump /dev/tty").decode("utf-8", "ignore")
    nodes = []
    for raw in re.findall(r"<node [^>]*>", xml):
        a = dict(re.findall(r'(\S+?)="([^"]*)"', raw))
        nodes.append(dict(text=a.get("text", ""), desc=a.get("content-desc", ""), id=a.get("resource-id", ""),
                          b=list(map(int, re.findall(r"\d+", a["bounds"])))))
    return nodes


def center(b):
    return (b[0] + b[2]) // 2, (b[1] + b[3]) // 2


def tap_xy(x, y, wait=1.5):
    adb(f"shell input tap {x} {y}")
    time.sleep(wait)


def by_id(nodes=None) -> dict:
    return {n["id"]: n for n in (nodes or dump()) if n["id"]}


def tap_id(tag: str, wait=1.5):
    node = by_id().get(tag)
    if not node:
        raise SystemExit(f"missing {tag}")
    tap_xy(*center(node["b"]), wait)


def tap_text(text: str):
    node = next((n for n in dump() if n["text"] == text), None)
    if not node:
        raise SystemExit(f"missing text {text}")
    tap_xy(*center(node["b"]))


def drag(a, b):
    """Slow drag via motion events; `input draganddrop` is too fast for Compose drop targets."""
    (x1, y1), (x2, y2) = center(a), center(b)
    steps = " ".join(f"input motionevent MOVE {x1 + (x2 - x1) * i // 20} {y1 + (y2 - y1) * i // 20};" for i in range(1, 21))
    adb(f'shell "input motionevent DOWN {x1} {y1}; sleep 0.8; {steps} input motionevent MOVE {x2 + 5} {y2 + 3}; '
        f'sleep 0.5; input motionevent UP {x2} {y2}"')
    time.sleep(1.5)


def capture(path: Path):
    path.write_bytes(adb("exec-out screencap -p"))
    ads = [n["b"] for n in dump() if "gclid" in n["desc"] or n["text"] in ("Test Ad", "Zkušební reklama", "Тестовое объявление")]
    box = [min(b[0] for b in ads), min(b[1] for b in ads), max(b[2] for b in ads), max(b[3] for b in ads)] if ads else None
    path.with_suffix(".ad.json").write_text(json.dumps({"ad": box}))
    print("captured", path.relative_to(ROOT.parent))


def load(name: str) -> dict:
    words = {}
    for line in (ASSETS / name).open(encoding="utf-8"):
        w = json.loads(line)
        words.setdefault(w["word"], w)
    return words


def home():
    for _ in range(5):
        d = by_id()
        if "hub_screen" in d:
            return
        if not any(i.startswith("com.usharik.app") or "_" in i and ":" not in i for i in d):
            adb("shell monkey -p com.usharik.app -c android.intent.category.LAUNCHER 1")  # app not in front
            time.sleep(6)
            continue
        if "full_quit_leave" in d:
            tap_xy(*center(d["full_quit_leave"]["b"]))
        elif "nav_home" in d:
            tap_xy(*center(d["nav_home"]["b"]))
        else:
            adb("shell input keyevent BACK")
            time.sleep(1)


def fill(answers: dict, keys: list[str]):
    for key in keys:
        time.sleep(1.2)
        d = dump()
        pool = [n for n in d if n["id"].startswith("full_pool_word_") and n["desc"] == answers[key]]
        cell = [n for n in d if n["id"] == "full_cell_" + key]
        if pool and cell:
            drag(pool[0]["b"], cell[0]["b"])
    print("  errors", by_id()["full_error_counter"]["text"])


def word() -> str:
    return by_id()["full_word"]["text"]


def subtitle() -> str:
    d = dump()
    w = next(n for n in d if n["id"] == "full_word")["b"]
    return " ".join(n["text"] for n in d if n["text"] and w[3] - 5 <= n["b"][1] < w[3] + 40)


def main() -> int:
    locale, language, *wanted = sys.argv[1:]
    wanted = set(wanted or ["hub", "noun", "adjective", "verb", "phrase", "handbook"])
    out = ROOT / "raw" / locale
    out.mkdir(parents=True, exist_ok=True)
    nouns, adjs, verbs = load("data.jsonl"), load("adjectives.jsonl"), load("verbs.jsonl")

    adb("shell pm grant com.usharik.app android.permission.POST_NOTIFICATIONS")
    adb("shell settings put global sysui_demo_allowed 1")
    for c in ("enter", "clock -e hhmm 0930", "notifications -e visible false",
              "battery -e level 100 -e plugged false", "network -e wifi show -e level 4"):
        adb(f"shell am broadcast -a com.android.systemui.demo -e command {c}")
    adb("shell monkey -p com.usharik.app -c android.intent.category.LAUNCHER 1")
    time.sleep(6)

    home()
    tap_id("btn_settings")
    tap_xy(250, 1690)  # "App language" row
    tap_text(language)
    time.sleep(3)
    home()

    if "noun" in wanted:
        tap_id("hub_pos_noun"); tap_id("btn_full"); tap_id("nav_next")
        while word() not in nouns or nouns[word()]["declensionType"] == "nesklonné":
            tap_id("nav_next")
        n = nouns[word()]
        fill({f"{a}_{c}": n["cases"][a][c] for a in (0, 1) for c in range(7)},
             ["0_0", "0_1", "0_2", "0_3", "1_0", "1_1", "1_3", "1_2", "0_4"])
        capture(out / "02_noun_table.png"); home()

    if "adjective" in wanted:
        tap_id("hub_pos_adjective"); tap_id("btn_full")
        for _ in range(25):
            tap_id("nav_next")
            w, m = word(), re.search(r"rod: ([^)]*)\)", subtitle())
            if w in adjs and adjs[w]["kind"] == "tvrdé" and m and m.group(1) in GENDER:
                break
        g, a = GENDER[m.group(1)], adjs[w]
        fill({f"{x}_{c}": a["cases"][x][g][c] for x in (0, 1) for c in range(7)},
             ["0_0", "0_1", "0_2", "0_3", "1_0", "1_1", "1_2", "1_3"])
        capture(out / "03_adjective_table.png"); home()

    if "verb" in wanted:
        tap_id("hub_pos_verb"); tap_id("btn_full"); tap_id("nav_next")
        while word() not in verbs:
            tap_id("nav_next")
        v = verbs[word()]
        p, pa = v["present"], v["past"]
        answers = {"0_0": p[0], "0_1": p[1], "0_2": p[2], "1_0": p[3], "1_1": p[4], "1_2": p[5],
                   "0_3": pa[0], "0_4": pa[1], "1_3": pa[3]}
        fill(answers, list(answers))
        capture(out / "04_verb_table.png"); home()

    if "phrase" in wanted:
        tap_id("hub_pos_phrase"); tap_id("btn_single")

        def options():
            d = dump()
            res = []
            for i in range(4):
                b = next(n for n in d if n["id"] == f"sc_answer_{i}")["b"]
                t = [n["text"] for n in d if n["text"] and b[0] <= n["b"][0] and n["b"][2] <= b[2]
                     and b[1] <= n["b"][1] and n["b"][3] <= b[3]]
                res.append((t[0] if t else "", b))
            return res

        for _ in range(10):
            phrase = by_id()["sc_word"]["text"]
            adj, noun = phrase.split(" ", 1)
            nn = nouns.get(noun)
            aa = [x for x in adjs.values() if nn and any(x["cases"][0][g][0] == adj for g in range(4))]
            if nn and aa and by_id()["sc_case_name"]["text"].startswith("1"):
                break
            tap_id("sc_next_word")
        g = GENDER[nn["gender"].replace("rod: ", "")]
        tap_xy(*center(next(b for t, b in options() if t == phrase)))
        tap_id("sc_next_case")
        gen_adj = aa[0]["cases"][0][g][1]
        gen_noun = [s.strip() for s in nn["cases"][0][1].split(",")]
        tap_xy(*center(next(b for t, b in options() if t.split(" ", 1)[0] == gen_adj and t.split(" ", 1)[1] in gen_noun)), 1)
        capture(out / "05_phrase_single.png"); home()

    if "handbook" in wanted:
        tap_id("btn_handbook"); tap_id("handbook_pos_adjective")
        capture(out / "06_handbook.png"); home()

    if "hub" in wanted:
        tap_id("hub_pos_adjective")
        capture(out / "01_hub.png")
    return 0


if __name__ == "__main__":
    sys.exit(main())
