#!/usr/bin/env python3
"""Build offline 604-page Madinah Quran assets from the preserved source text."""
import argparse
import json
import re
import unicodedata
from pathlib import Path

BASMALA = "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَـٰنِ ٱلرَّحِیمِ"
BASMALA_PREFIX_WORDS = 4


def norm_word(value):
    out = []
    for char in unicodedata.normalize("NFKD", value):
        if char == "ـ" or unicodedata.category(char) in ("Mn", "Me", "Cf"):
            continue
        if char in "ٱأإآٲ":
            char = "ا"
        elif char in "ىيئ":
            char = "ي"
        elif char == "ک":
            char = "ك"
        elif char == "ہ":
            char = "ه"
        if char.isspace():
            continue
        out.append(char)
    return "".join(out)


def arabic_digits(number):
    return str(number).translate(str.maketrans("0123456789", "٠١٢٣٤٥٦٧٨٩"))


def read_json(path):
    with Path(path).open("r", encoding="utf-8") as handle:
        return json.load(handle)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--layout", required=True, help="Path to madinah-mushaf-json/pages")
    parser.add_argument("--engine-data", required=True, help="Path to Quran-Tajweed-Engine/data")
    args = parser.parse_args()

    root = Path(__file__).resolve().parents[1]
    original_path = root / "quran-uthmani-kitab.txt"
    original_lines = original_path.read_text(encoding="utf-8").splitlines()
    if len(original_lines) != 6236:
        raise SystemExit(f"Expected 6236 Quran lines; found {len(original_lines)}")

    quran = read_json(Path(args.engine_data) / "quran.json")
    if isinstance(quran, dict):
        surahs = quran.get("surahs") or quran.get("chapters") or []
    else:
        surahs = quran
    if len(surahs) != 114:
        raise SystemExit(f"Expected 114 surahs in reference data; found {len(surahs)}")

    verses = []
    for surah in sorted(surahs, key=lambda item: int(item.get("id", 0))):
        surah_number = int(surah["id"])
        for ayah_number, ayah in enumerate(surah.get("ayahs", []), start=1):
            source_text = ayah.get("textArabic") or ayah.get("text") or ""
            if not source_text:
                raise SystemExit(f"Missing reference Arabic text at {surah_number}:{ayah_number}")
            verses.append((surah_number, ayah_number, source_text))

    if len(verses) != 6236:
        raise SystemExit(f"Expected 6236 reference ayahs; found {len(verses)}")

    original_words = {}
    basmala_words = {}
    mismatches = []
    for index, (surah_number, ayah_number, source_text) in enumerate(verses):
        raw_words = re.split(r" +", original_lines[index].strip())
        if ayah_number == 1 and surah_number not in (1, 9):
            if len(raw_words) <= BASMALA_PREFIX_WORDS:
                mismatches.append(f"{surah_number}:{ayah_number} has no room for basmala prefix")
                continue
            prefix = raw_words[:BASMALA_PREFIX_WORDS]
            target_words = raw_words[BASMALA_PREFIX_WORDS:]
            if norm_word("".join(prefix)) != norm_word("".join(BASMALA.split())):
                mismatches.append(f"{surah_number}:{ayah_number} basmala prefix differs")
                continue
            basmala_words[surah_number] = " ".join(prefix)
        else:
            target_words = raw_words

        source_words = re.split(r" +", source_text.strip())
        if len(source_words) != len(target_words):
            mismatches.append(
                f"{surah_number}:{ayah_number} word count reference={len(source_words)} source={len(target_words)}"
            )
            continue
        if any(norm_word(a) != norm_word(b) for a, b in zip(source_words, target_words)):
            mismatches.append(f"{surah_number}:{ayah_number} word spelling/order mismatch")
            continue
        original_words[(surah_number, ayah_number)] = target_words

    if mismatches:
        raise SystemExit("Quran text alignment validation failed; no color data was generated:\n" + "\n".join(mismatches[:20]))

    rules = read_json(Path(args.engine_data) / "tajweed-rules.json")
    colors = {}
    legend = []
    for category in rules.get("categories", []):
        rule_id = category.get("id")
        color = category.get("colorHex")
        if rule_id and color:
            colors[rule_id] = color
            if category.get("arabicTitle"):
                legend.append({
                    "name": category["arabicTitle"],
                    "color": color,
                    "description": category.get("shortDescription") or ""
                })

    word_colors = {}
    for surah_number in range(1, 115):
        annotation_path = Path(args.engine_data) / "tajweed" / f"{surah_number:03d}.json"
        annotation_data = read_json(annotation_path)
        for verse_data in annotation_data:
            ayah_number = int(verse_data["ayah"])
            source_text = next(
                text for s, a, text in verses if s == surah_number and a == ayah_number
            )
            source_words = list(re.finditer(r"[^ ]+", source_text.strip()))
            chosen = {}
            for annotation in verse_data.get("annotations", []):
                start = int(annotation["start"])
                end = int(annotation["end"])
                rule = annotation.get("rule", "")
                if rule not in colors:
                    continue
                for word_index, match in enumerate(source_words):
                    overlap = max(0, min(end, match.end()) - max(start, match.start()))
                    if overlap <= 0:
                        continue
                    previous = chosen.get(word_index)
                    rank = overlap * 1000 - int(next(
                        (c.get("sortRank", 999) for c in rules.get("categories", []) if c.get("id") == rule),
                        999
                    ))
                    if previous is None or rank > previous[0]:
                        chosen[word_index] = (rank, colors[rule])
            word_colors[(surah_number, ayah_number)] = {
                word_index: color for word_index, (_, color) in chosen.items()
            }

    layout_dir = Path(args.layout)
    verse_pages = [0] * len(verses)
    verse_index = {(s, a): i for i, (s, a, _) in enumerate(verses)}
    pages_out = []

    for page_number in range(1, 605):
        page_path = layout_dir / f"page-{page_number:03d}.json"
        page_data = read_json(page_path)
        page_verses = page_data.get("verses", [])
        for verse in page_verses:
            key = (int(verse["surah_number"]), int(verse["ayah_number"]))
            idx = verse_index.get(key)
            if idx is None:
                raise SystemExit(f"Layout references unknown verse {key}")
            if verse_pages[idx] == 0:
                verse_pages[idx] = page_number

        source_layout_lines = page_data.get("lines", [])
        output_lines = []
        for line_index, line in enumerate(source_layout_lines):
            line_type = line.get("type")
            if line_type == "surah-header":
                output_lines.append({"type": "header", "text": line.get("text", "")})
                next_verse = None
                for later in source_layout_lines[line_index + 1:]:
                    if later.get("type") == "ayah-line" and later.get("tokens"):
                        token = later["tokens"][0]
                        next_verse = (int(token["s"]), int(token["a"]))
                        break
                    if later.get("type") == "basmala":
                        break
                if next_verse and next_verse[1] == 1 and next_verse[0] not in (1, 9):
                    output_lines.append({
                        "type": "basmala",
                        "text": basmala_words.get(next_verse[0], BASMALA)
                    })
            elif line_type == "basmala":
                # Some layout editions encode the basmala as its own line.
                output_lines.append({"type": "basmala", "text": line.get("text") or BASMALA})
            elif line_type == "ayah-line":
                rendered_words = []
                for token in line.get("tokens", []):
                    s, a, w = int(token["s"]), int(token["a"]), int(token["w"])
                    words = original_words.get((s, a))
                    if words is None or w < 0 or w >= len(words):
                        raise SystemExit(f"Invalid word token on page {page_number}: {s}:{a}:{w}")
                    rendered_words.append({
                        "text": words[w],
                        "color": word_colors.get((s, a), {}).get(w, ""),
                        "endVerse": w == len(words) - 1,
                        "ayah": a
                    })
                if rendered_words:
                    output_lines.append({"type": "ayah-line", "words": rendered_words})
            else:
                raise SystemExit(f"Unknown line type on page {page_number}: {line_type}")

        pages_out.append({"page": page_number, "lines": output_lines})

    if any(page == 0 for page in verse_pages):
        missing = [verses[i][:2] for i, page in enumerate(verse_pages) if page == 0][:20]
        raise SystemExit(f"Some ayahs have no page assignment: {missing}")

    output_dir = root / "app" / "build" / "generated" / "quranAssets"
    output_dir.mkdir(parents=True, exist_ok=True)
    payload = {
        "pages": pages_out,
        "versePages": verse_pages,
        "legend": legend
    }
    (output_dir / "quran-pages.json").write_text(
        json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        encoding="utf-8"
    )
    print(
        f"Prepared {len(pages_out)} Madinah pages, {len(verses)} validated ayahs, "
        f"{sum(len(v) for v in word_colors.values())} colored words, and {len(legend)} legend rules."
    )


if __name__ == "__main__":
    main()
