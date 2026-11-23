"""Writes the inverse of a BML remap (target build -> source build), with H rows taken from the target jar.
Usage: python3 invert_remap.py <remap a__b.tsv.gz> <jar of b> <out b__a.tsv.gz>"""
import sys, os, gzip, zipfile, importlib.util
BML = os.environ.get("BML", "/Applications/BML-0.36.0-dev.13-macos")  # same default as the shell scripts
spec = importlib.util.spec_from_file_location("js", os.path.join(BML, "docs/sdk/jarsearch.py"))
js = importlib.util.module_from_spec(spec); spec.loader.exec_module(js)
src, jarp, out = sys.argv[1:4]
rows, seen = [], {}
for line in gzip.open(src, "rt", encoding="utf-8"):
    p = line.rstrip("\n").split("\t")
    if p[0] == "C":
        key = ("C", p[2]); val = p[1]
        rows.append(f"C\t{p[2]}\t{p[1]}")
    elif p[0] in ("M", "F"):
        key = (p[0], p[4], p[5], p[6]); val = (p[1], p[2], p[3])
        rows.append(f"{p[0]}\t{p[4]}\t{p[5]}\t{p[6]}\t{p[1]}\t{p[2]}\t{p[3]}")
    else:
        continue  # H rows describe the old source build; new ones come from the jar below
    if key in seen and seen[key] != val:
        sys.exit(f"NOT_INVERTIBLE {key}: {seen[key]} vs {val}")
    seen[key] = val
h = 0
with zipfile.ZipFile(jarp) as z:
    for n in z.namelist():
        if not n.endswith(".class"): continue
        try: _, _, name, sup, ifs = js.read_class(z.read(n))
        except Exception: continue
        for parent in ([sup] if sup else []) + ifs:
            rows.append(f"H\t{name}\t{parent}"); h += 1
with gzip.open(out, "wt", encoding="utf-8") as f:
    f.write("\n".join(dict.fromkeys(rows)) + "\n")
print("INVERTED", len(seen), "mappings,", h, "H rows ->", out)
