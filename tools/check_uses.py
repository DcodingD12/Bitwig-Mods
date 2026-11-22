"""Read-only availability check, like BML's mod_uses.py check: does every Bitwig name a mod uses
(its uses.tsv plus the hook targets in its hooks.json) have a counterpart in a map pack's remap?

  python3 check_uses.py <uses.tsv> <hooks.json> <remap from__to.tsv.gz> [<remap> ...]

Prints USES_OK <remap> or the missing names per remap. Writes nothing.
"""
import gzip
import json
import sys


def needed(uses_path, hooks_path):
    names = set()
    with open(uses_path, encoding="utf-8") as f:
        for line in f:
            p = line.rstrip("\n").split("\t")
            if p[0] == "C":
                names.add(("C", p[1]))
            elif p[0] in ("M", "F"):
                names.add((p[0], p[4], p[2], p[3]))  # keyed by the declaring class
    with open(hooks_path, encoding="utf-8") as f:
        hooks = json.load(f)

    def add(t):
        if t["kind"] == "class":
            names.add(("C", t["class"]))
        else:
            names.add(("M" if t["kind"] == "method" else "F", t["owner"], t["name"], t["descriptor"]))
            names.add(("C", t["owner"]))

    for h in hooks.get("hooks", []):
        add(h["target"])
        if "match" in h and "target" in h["match"]:
            add(h["match"]["target"])
    for a in hooks.get("access", []):
        add(a)
    return names


def check(names, remap):
    have = set()
    with gzip.open(remap, "rt", encoding="utf-8") as f:
        for line in f:
            p = line.rstrip("\n").split("\t")
            if p[0] == "C":
                have.add(("C", p[1]))
            elif p[0] in ("M", "F"):
                have.add((p[0], p[1], p[2], p[3]))
    return sorted(n for n in names if n not in have and not (n[0] == "C" and "/" in n[1] and n[1].startswith("java/")))


def main():
    if len(sys.argv) < 4:
        sys.exit(__doc__)
    names = needed(sys.argv[1], sys.argv[2])
    bad = 0
    for remap in sys.argv[3:]:
        missing = check(names, remap)
        if missing:
            bad += 1
            print("USES_MISSING", remap)
            for m in missing:
                print("   ", " ".join(m))
        else:
            print("USES_OK", len(names), "names", remap)
    sys.exit(1 if bad else 0)


if __name__ == "__main__":
    main()
