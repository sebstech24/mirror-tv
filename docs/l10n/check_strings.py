#!/usr/bin/env python3
"""Compare translated strings.xml files against English: names, placeholders, escapes.
Usage: python3 docs/l10n/check_strings.py values-de values-fr ..."""
import re, sys, xml.etree.ElementTree as ET
from collections import Counter

import os
RES = os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../app/src/main/res/")
PH = re.compile(r"%(?:\d+\$)?[sd]|%%")

def load(path):
    root = ET.parse(path).getroot()
    out = {}
    for el in root:
        if el.get("translatable") == "false":
            continue
        name = el.get("name")
        if el.tag == "string":
            out[name] = ("string", ["".join(el.itertext())])
        elif el.tag == "string-array":
            out[name] = ("array", ["".join(i.itertext()) for i in el])
        elif el.tag == "plurals":
            out[name] = ("plurals", {i.get("quantity"): "".join(i.itertext()) for i in el})
    return out

def main(folder):
    errs = []
    path = RES + folder + "/strings.xml"
    raw = open(path, encoding="utf-8").read()
    for n, line in enumerate(raw.splitlines(), 1):
        body = re.sub(r"<[^>]+>", "", line)
        if re.search(r"(?<!\\)'", body):
            errs.append(f"line {n}: unescaped apostrophe")
        if re.search(r'(?<!\\)"', body):
            errs.append(f"line {n}: straight double quote in text")
    try:
        en, tr = load(RES + "values/strings.xml"), load(path)
    except ET.ParseError as e:
        print("XML ERROR", e); sys.exit(1)
    for name in en.keys() - tr.keys():
        errs.append(f"missing: {name}")
    for name in tr.keys() - en.keys():
        errs.append(f"extra or untranslatable: {name}")
    for name in en.keys() & tr.keys():
        kind, ev = en[name]
        tkind, tv = tr[name]
        if kind != tkind:
            errs.append(f"{name}: wrong element type"); continue
        if kind == "string":
            if Counter(PH.findall(ev[0])) != Counter(PH.findall(tv[0])):
                errs.append(f"{name}: placeholders {PH.findall(ev[0])} vs {PH.findall(tv[0])}")
        elif kind == "array":
            if len(ev) != len(tv):
                errs.append(f"{name}: {len(tv)} items, need {len(ev)}")
        else:
            want = Counter(PH.findall(ev["other"]))
            if "other" not in tv:
                errs.append(f"{name}: no 'other' item")
            for q, text in tv.items():
                if Counter(PH.findall(text)) != want:
                    errs.append(f"{name}[{q}]: placeholders {PH.findall(text)}")
    print("\n".join(errs) if errs else f"OK {folder}: {len(tr)} entries")
    return bool(errs)

if __name__ == "__main__":
    bad = [f for f in sys.argv[1:] if main(f)]
    sys.exit(1 if bad else 0)
