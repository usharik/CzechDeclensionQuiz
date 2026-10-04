#!/usr/bin/env python3
"""Adds Ukrainian and Vietnamese glosses to the bundled dictionaries.

The glosses live in translations/{nouns,adjectives,verbs}.txt, one `word | uk | vi` line per entry,
and are written into each JSONL record as `translation_uk` and `translation_vi` right after
`translation_en`. build_corpus.py applies them to the adjectives and verbs it generates; the noun
corpus (data.jsonl) is not generated, so running this script updates all three files in place.

Usage: python3 add_translations.py         (fails if any dictionary word has no gloss)
"""
import json, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.normpath(os.path.join(HERE, "..", "..", "database", "src", "main", "assets"))
TRANSLATIONS = os.path.join(HERE, "translations")
# asset file -> gloss file
CORPORA = {"data.jsonl": "nouns.txt", "adjectives.jsonl": "adjectives.txt", "verbs.jsonl": "verbs.txt"}


def load_glosses(name):
    """{czech word: (uk, vi)} from one translations/*.txt file."""
    glosses = {}
    with open(os.path.join(TRANSLATIONS, name)) as f:
        for n, line in enumerate(f, 1):
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            parts = [p.strip() for p in line.split("|")]
            if len(parts) != 3 or not all(parts):
                sys.exit(f"{name}:{n}: expected 'word | uk | vi', got {line!r}")
            if parts[0] in glosses:
                sys.exit(f"{name}:{n}: duplicate word {parts[0]!r}")
            glosses[parts[0]] = (parts[1], parts[2])
    return glosses


def with_translations(record, glosses):
    """A copy of [record] with translation_uk/translation_vi inserted after translation_en."""
    uk, vi = glosses[record["word"]]
    out = {}
    for key, value in record.items():
        if key in ("translation_uk", "translation_vi"):
            continue
        out[key] = value
        if key == "translation_en":
            out["translation_uk"], out["translation_vi"] = uk, vi
    return out


def apply(records, gloss_file):
    """Adds the glosses to [records]; exits listing the words that have none."""
    glosses = load_glosses(gloss_file)
    missing = [r["word"] for r in records if r["word"] not in glosses]
    if missing:
        sys.exit(f"{gloss_file}: no Ukrainian/Vietnamese gloss for {len(missing)} words: {missing[:40]}")
    unused = set(glosses) - {r["word"] for r in records}
    if unused:
        print(f"{gloss_file}: {len(unused)} glosses for words not in the corpus: {sorted(unused)[:20]}")
    return [with_translations(r, glosses) for r in records]


def main():
    for asset, gloss_file in CORPORA.items():
        path = os.path.join(ASSETS, asset)
        with open(path) as f:
            text = f.read()
        records = apply([json.loads(line) for line in text.splitlines() if line.strip()], gloss_file)
        lines = [json.dumps(r, ensure_ascii=False) for r in records]
        with open(path, "w") as f:
            # Keep the file's own trailing-newline convention so the diff stays minimal.
            f.write("\n".join(lines) + ("\n" if text.endswith("\n") else ""))
        print(f"{asset}: added uk/vi glosses to {len(records)} entries")


if __name__ == "__main__":
    main()
