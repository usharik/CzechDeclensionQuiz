#!/usr/bin/env python3
"""Builds the adjective and verb corpora shipped in database/src/main/assets.

Sources (all CC BY-SA):
  * Czech Wiktionary  - full inflection tables ({{Adjektivum (cs)}}, {{Sloveso (cs)}}), Russian/English glosses.
  * English Wiktionary - English glosses and aspect partners ({{cs-verb|a=impf|pf=...}}).
  * Russian Wiktionary - Russian glosses where the Czech Wiktionary has none.
  * hermitdave/FrequencyWords (OpenSubtitles 2018, cs_50k) - frequency ranking used to pick common lemmas.
Hand-written glosses and exclusions live in manual_translations.py; Ukrainian and Vietnamese glosses
live in translations/*.txt (see add_translations.py).

Usage: python3 build_corpus.py            (uses ./cache, fetches whatever is missing)
"""
import json, os, re, sys, time, urllib.parse, urllib.request

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from parse_wikitext import parse_adj, parse_verb  # noqa: E402
import manual_translations as mt  # noqa: E402
import add_translations  # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))
CACHE = os.path.join(HERE, "cache")
ASSETS = os.path.normpath(os.path.join(HERE, "..", "..", "database", "src", "main", "assets"))
UA = {"User-Agent": "CzechDeclensionQuiz-corpus/1.0 (https://github.com/usharik/CzechDeclensionQuiz)"}
VERB_TARGET, ADJ_TARGET = 470, 500

MODEL_VERBS = ["být", "mít", "dělat", "prosit", "mluvit", "kupovat", "tisknout", "nést", "číst", "jít", "jet", "chtít",
               "vědět", "jíst", "moct", "psát", "brát", "krýt", "mazat", "péct", "trpět", "sázet", "umět", "rozumět",
               "minout", "začít", "pít", "spát", "vařit", "platit", "studovat", "zpívat", "tančit", "plavat", "prodávat",
               "prodat", "sníst", "malovat", "odpovídat", "říct", "stát", "vzít", "dát", "dávat", "koupit", "vidět", "slyšet"]
MODEL_ADJS = ["mladý", "jarní", "otcův", "matčin", "dobrý", "velký", "malý", "nový", "starý", "cizí", "poslední", "moderní",
              "bratrův", "sestřin", "dědečkův", "babiččin"]
# Verbs that legitimately have no imperative; their imperative cells stay empty in the quiz.
NO_IMPERATIVE_OK = {"muset", "moct", "chtít", "smět", "umět", "lze"}
IRREGULAR = ("být", "mít", "chtít", "vědět", "jíst", "moct", "jít", "říct", "spát", "stát")


def load_cache(name):
    p = os.path.join(CACHE, name)
    return json.load(open(p)) if os.path.exists(p) else {}


def save_cache(name, data):
    json.dump(data, open(os.path.join(CACHE, name), "w"), ensure_ascii=False)


def fetch_missing(site, titles):
    cache_name = f"{site}_wikitext.json"
    data = load_cache(cache_name)
    todo = [t for t in dict.fromkeys(titles) if t not in data]
    for i in range(0, len(todo), 50):
        chunk = todo[i:i + 50]
        p = {"action": "query", "prop": "revisions", "rvprop": "content", "rvslots": "main", "titles": "|".join(chunk),
             "format": "json", "formatversion": "2"}
        req = urllib.request.Request(f"https://{site}.wiktionary.org/w/api.php?" + urllib.parse.urlencode(p), headers=UA)
        for attempt in range(5):
            try:
                d = json.load(urllib.request.urlopen(req, timeout=90))
                break
            except Exception as e:  # noqa: BLE001
                print("retry", site, attempt, e, file=sys.stderr)
                time.sleep(3 * (attempt + 1))
        else:
            continue
        for pg in d["query"]["pages"]:
            data[pg["title"]] = pg["revisions"][0]["slots"]["main"]["content"] if "revisions" in pg else ""
        time.sleep(0.4)
    if todo:
        save_cache(cache_name, data)
    return data


def frequency_rank():
    rank = {}
    for i, line in enumerate(open(os.path.join(CACHE, "cs_50k.txt"))):
        rank.setdefault(line.split()[0], i)
    return rank


def ru_wiktionary_gloss(text):
    m = re.search(r"= \{\{-cs-\}\} =(.*?)(?=\n= \{\{-|\Z)", text or "", re.S)
    if not m:
        return ""
    zm = re.search(r"==== Значение ====\n(.*?)(?=\n====|\n===|\Z)", m.group(1), re.S)
    if not zm:
        return ""
    out = []
    for line in zm.group(1).split("\n"):
        if not line.startswith("# "):
            continue
        g = line[2:]
        while re.search(r"\{\{[^{}]*\}\}", g):  # templates nest ({{пример|...{{...}}...}})
            g = re.sub(r"\{\{[^{}]*\}\}", "", g)
        g = re.sub(r"\[\[([^\]|]*\|)?([^\]]*)\]\]", r"\2", g)
        g = re.sub(r"<[^>]*>", "", g).strip(" .;,")
        if g and len(out) < 2:
            out.append(g)
    return "; ".join(out)


def split_senses(gloss):
    """Splits on ';' outside parentheses."""
    out, depth, cur = [], 0, ""
    for ch in gloss:
        depth += ch == "("
        depth -= ch == ")"
        if ch == ";" and depth <= 0:
            out.append(cur)
            cur = ""
        else:
            cur += ch
    out.append(cur)
    return out


def shorten(gloss, max_len=60):
    """Keeps the first distinct senses that fit, dropping parenthesised explanations when needed."""
    gloss = re.sub(r"\(\s*\)", "", gloss)
    gloss = re.sub(r"\([^()]{30,}\)", "", gloss)  # long explanations never help on a quiz card
    senses = [s.strip(" ,:") for s in split_senses(gloss) if s.strip(" ,:")]
    senses = list(dict.fromkeys(senses))
    if len("; ".join(senses)) > max_len:
        senses = [re.sub(r"\s*\([^)]*\)", "", s).strip() or s for s in senses]
        senses = list(dict.fromkeys(s for s in senses if s))
    out = []
    for s in senses:
        if out and len("; ".join(out + [s])) > max_len:
            break
        out.append(s)
    return "; ".join(out[:3])


def english(entry, manual):
    if entry["word"] in mt.EN_OVERRIDES:
        return mt.EN_OVERRIDES[entry["word"]]
    if entry["word"] in manual:
        return manual[entry["word"]]
    g = entry.get("en") or entry.get("en_cs") or ""
    g = re.sub(r"\s+", " ", g).strip(" ;")
    return shorten(g)


def russian(entry, manual, ru_pages):
    if entry["word"] in mt.RU_OVERRIDES:
        return mt.RU_OVERRIDES[entry["word"]]
    if entry["word"] in manual:
        return manual[entry["word"]]
    return shorten(entry.get("ru") or ru_wiktionary_gloss(ru_pages.get(entry["word"], "")), 70)


def verb_class(word, present3):
    base = present3.split(",")[0].strip()
    base = re.sub(r"\s+(se|si)$", "", base)
    stem_word = re.sub(r"\s+(se|si)$", "", word)
    if any(stem_word == w or (stem_word.endswith(w) and w in ("jít", "jíst", "vědět", "moct", "říct")) for w in IRREGULAR):
        return "nepravidelné"
    if base.endswith("á"):
        return "dělá"
    if base.endswith("í"):
        return "prosí"
    if base.endswith("uje"):
        return "kupuje"
    if base.endswith("ne"):
        return "tiskne"
    if base.endswith("e"):
        return "nese"
    return "nepravidelné"


def reflexive_from_base(word, base_entry):
    particle = word.split()[-1]
    def add(forms):
        return [f"{f} {particle}" if f else "" for f in forms]
    e = dict(base_entry)
    e["word"] = word
    e["present"] = add(base_entry["present"])
    e["past"] = add(base_entry["past"])
    e["imperative"] = add(base_entry["imperative"])
    e["future"] = add(base_entry["future"]) if any(base_entry["future"]) else base_entry["future"]
    e["ru"], e["en"], e["en_cs"] = "", "", ""
    head = dict(base_entry.get("head") or {})
    for k in ("pf", "impf"):
        if k in head:
            head[k] = ", ".join(f"{p.strip()} {particle}" for p in head[k].split(","))
    e["head"] = head
    return e


def build_verbs(rank, cs, en, ru):
    candidates = [w.strip() for w in open(os.path.join(CACHE, "cs_verbs.txt")) if w.strip()]
    plain = sorted((rank[w], w) for w in candidates if " " not in w and w in rank and w[0].islower())
    plain = [w for _, w in plain if w not in mt.EXCLUDED_VERBS]
    curated_refl = json.load(open(os.path.join(HERE, "curated_reflexives.json")))
    refl_bases = [w.split()[0] for w in curated_refl]
    wanted = list(dict.fromkeys(plain[:VERB_TARGET + 150] + MODEL_VERBS + curated_refl + refl_bases))
    cs = fetch_missing("cs", wanted)
    en = fetch_missing("en", wanted)

    def parsed(word):
        e = parse_verb(word, cs.get(word, ""), en.get(word, ""))
        if not e:
            return None
        for k, v in mt.VERB_FIXES.get(word, {}).items():
            e[k] = v
        return e

    def complete(e):
        return e and all(e["present"]) and all(e["past"]) and (all(e["imperative"]) or e["word"] in NO_IMPERATIVE_OK)

    chosen = []
    for w in plain:
        e = parsed(w)
        if complete(e):
            chosen.append(e)
        if len(chosen) >= VERB_TARGET:
            break
    have = {e["word"] for e in chosen}
    for w in MODEL_VERBS:
        if w not in have:
            e = parsed(w)
            if complete(e):
                chosen.append(e)
                have.add(w)
    for w in curated_refl:
        e = parsed(w)
        if not complete(e) and w in mt.MANUAL_VERBS:
            e = dict(mt.MANUAL_VERBS[w], word=w, ru="", en="", en_cs="", future=["", "", "", "", "", ""])
        if not complete(e):
            base = parsed(w.split()[0])
            if not complete(base):
                print("reflexive skipped (no base):", w, file=sys.stderr)
                continue
            e = reflexive_from_base(w, base)
        if w not in have:
            chosen.append(e)
            have.add(w)
    ru = fetch_missing("ru", [e["word"] for e in chosen])
    out = []
    for i, e in enumerate(chosen, 1):
        aspect = mt.ASPECT_FIXES.get(e["word"], {"impf": "nedokonavé", "pf": "dokonavé", "bi": "obouvidové"}.get(e["aspect"], ""))
        head = e.get("head") or {}
        pair = head.get("pf") if e["aspect"] == "impf" else head.get("impf") if e["aspect"] == "pf" else None
        if pair:
            pair = re.sub(r"\[\[|\]\]", "", pair).split(",")[0].strip()
        pair = mt.PAIR_FIXES.get(e["word"], pair)
        rec = {
            "wordId": i, "word": e["word"], "aspect": aspect, "pair": pair or "",
            "verbClass": verb_class(e["word"], e["present"][2]),
            "translation_ru": russian(e, mt.RU_VERBS, ru), "translation_en": english(e, mt.EN_VERBS),
            "present": e["present"], "past": e["past"], "imperative": e["imperative"],
        }
        if any(e["future"]):
            rec["future"] = e["future"]
        out.append(rec)
    return out


def adj_kind(cases):
    nom = cases[0][0][0].split(",")[0].strip()
    if nom.endswith("ý"):
        return "tvrdé"
    if nom.endswith("í"):
        return "měkké"
    return "přivlastňovací"


def build_adjs(rank, cs, en, ru):
    candidates = [w.strip() for w in open(os.path.join(CACHE, "cs_adjs.txt")) if w.strip()]
    plain = sorted((rank[w], w) for w in candidates if " " not in w and w in rank and w[0].islower())
    plain = [w for _, w in plain if w not in mt.EXCLUDED_ADJS]
    wanted = list(dict.fromkeys(plain[:ADJ_TARGET + 150] + MODEL_ADJS))
    cs = fetch_missing("cs", wanted)
    en = fetch_missing("en", wanted)

    def complete(e):
        return e and all(c for num in e["cases"] for row in num for c in row) and e["kind"] != "nesklonné"

    chosen = []
    for w in plain:
        e = parse_adj(w, cs.get(w, ""), en.get(w, ""))
        if complete(e):
            chosen.append(e)
        if len(chosen) >= ADJ_TARGET:
            break
    have = {e["word"] for e in chosen}
    for w in MODEL_ADJS:
        if w not in have:
            e = parse_adj(w, cs.get(w, ""), en.get(w, ""))
            if complete(e):
                chosen.append(e)
    ru = fetch_missing("ru", [e["word"] for e in chosen])
    out = []
    for i, e in enumerate(chosen, 1):
        out.append({
            "wordId": i, "word": e["word"], "kind": adj_kind(e["cases"]),
            "translation_ru": russian(e, mt.RU_ADJS, ru), "translation_en": english(e, mt.EN_ADJS),
            "comparative": e["comparative"], "superlative": e["superlative"], "cases": e["cases"],
        })
    return out


def write_jsonl(name, records):
    path = os.path.join(ASSETS, name)
    with open(path, "w") as f:
        for r in records:
            f.write(json.dumps(r, ensure_ascii=False) + "\n")
    print(f"wrote {len(records)} records to {path}")


def report(name, records):
    no_ru = [r["word"] for r in records if not r["translation_ru"]]
    no_en = [r["word"] for r in records if not r["translation_en"]]
    print(f"{name}: {len(records)} entries; missing ru: {len(no_ru)} {no_ru[:40]}; missing en: {len(no_en)} {no_en[:40]}")


if __name__ == "__main__":
    rank = frequency_rank()
    cs, en, ru = load_cache("cs_wikitext.json"), load_cache("en_wikitext.json"), load_cache("ru_wikitext.json")
    verbs = build_verbs(rank, cs, en, ru)
    adjs = build_adjs(rank, cs, en, ru)
    report("verbs", verbs)
    report("adjectives", adjs)
    from collections import Counter
    print("verb classes:", Counter(v["verbClass"] for v in verbs))
    print("aspects:", Counter(v["aspect"] for v in verbs), "with pair:", sum(1 for v in verbs if v["pair"]))
    print("adjective kinds:", Counter(a["kind"] for a in adjs))
    # Ukrainian and Vietnamese glosses are hand-written in translations/*.txt; a new word fails here until it gets one.
    write_jsonl("verbs.jsonl", add_translations.apply(verbs, "verbs.txt"))
    write_jsonl("adjectives.jsonl", add_translations.apply(adjs, "adjectives.txt"))
