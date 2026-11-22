"""Translates the Bitwig names in a mod kit hooks.json with a BML remap (TSV, may be .gz).

  python3 translate_hooks.py <hooks.json> <remap a__b.tsv.gz> <out hooks.json>

Every class, method and field target (and every class inside a descriptor) is renamed from
build a to build b. A member must have a row in the remap for exactly that owner; otherwise the
script stops (no guessing). Used to move a mod built against 6.1.7 to 6.1.4 names, with the
inverse of BML's macos-6.1.4__macos-6.1.7 remap (see NOTES.md).
"""
import gzip
import json
import re
import sys


def load(path):
    opener = gzip.open if path.endswith(".gz") else open
    classes, members = {}, {}
    with opener(path, "rt", encoding="utf-8") as f:
        for line in f:
            p = line.rstrip("\n").split("\t")
            if p[0] == "C":
                classes[p[1]] = p[2]
            elif p[0] in ("M", "F"):
                members[(p[0], p[1], p[2], p[3])] = (p[4], p[5], p[6])
    return classes, members


def translate(hooks, classes, members):
    def cls(name):
        return classes.get(name, name)

    def desc(d):
        return re.sub(r"L([^;]+);", lambda m: "L" + cls(m.group(1)) + ";", d)

    def target(t):
        t = dict(t)
        if t["kind"] == "class":
            t["class"] = cls(t["class"])
            return t
        kind = "M" if t["kind"] == "method" else "F"
        row = members.get((kind, t["owner"], t["name"], t["descriptor"]))
        if row is None:
            raise SystemExit(f"NO_ROW {kind} {t['owner']}.{t['name']}{t['descriptor']}")
        t["owner"], t["name"], t["descriptor"] = row
        return t

    out = json.loads(json.dumps(hooks))
    for h in out.get("hooks", []):
        h["target"] = target(h["target"])
        if "method_from" in h:
            h["method_from"] = target(h["method_from"])
        if "match" in h:
            m = h["match"]
            if "target" in m:
                m["target"] = target(m["target"])
                m["owner"], m["name"], m["descriptor"] = m["target"]["owner"], m["target"]["name"], m["target"]["descriptor"]
        h["bridge"]["descriptor"] = desc(h["bridge"]["descriptor"])
    out["access"] = [target(a) for a in out.get("access", [])]
    for f in out.get("fields", []):
        f["owner"] = cls(f["owner"])
        f["descriptor"] = desc(f["descriptor"])
    return out


def main():
    if len(sys.argv) != 4:
        sys.exit(__doc__)
    with open(sys.argv[1], encoding="utf-8") as f:
        hooks = json.load(f)
    classes, members = load(sys.argv[2])
    result = translate(hooks, classes, members)
    with open(sys.argv[3], "w", encoding="utf-8") as f:
        json.dump(result, f, indent=2)
        f.write("\n")
    print("TRANSLATE_HOOKS_OK", sys.argv[3])


if __name__ == "__main__":
    main()
