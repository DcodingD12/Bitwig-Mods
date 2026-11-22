"""Writes uses.tsv for a helper jar in source names, using the real jar of another build plus
the remap source->that build to find each member's declaring class.
Usage: python3 make_uses.py <helper.jar (source names)> <remap source__real.tsv.gz> <real bitwig.jar> <out uses.tsv>"""
import sys, gzip, zipfile, subprocess, re, os, tempfile
helper, remap, realjar, out = sys.argv[1:5]
JAVAP = "/opt/homebrew/opt/openjdk@25/bin/javap"
fwd_c, fwd_m, inv_c = {}, {}, {}
for line in gzip.open(remap, "rt", encoding="utf-8"):
    p = line.rstrip("\n").split("\t")
    if p[0] == "C": fwd_c[p[1]] = p[2]; inv_c[p[2]] = p[1]
    elif p[0] == "M": fwd_m[(p[1], p[2], p[3])] = (p[4], p[5], p[6])

def refs(jar):
    """Class/member references of the helper (javap -v), Bitwig ones only."""
    found = set()
    with tempfile.TemporaryDirectory() as d:
        subprocess.run(["unzip", "-q", jar, "-d", d], check=True)
        for root, _, files in os.walk(d):
            for f in files:
                if not f.endswith(".class"): continue
                txt = subprocess.run([JAVAP, "-v", os.path.join(root, f)], capture_output=True, text=True).stdout
                for m in re.finditer(r"= (Class|Methodref|InterfaceMethodref|Fieldref)\s+\S+\s+// (\S+)", txt):
                    found.add((m.group(1), m.group(2).strip('"')))
    return found

skip = ("java/", "bml/", "bitwigmods/", "[")
cache = {}
def declared(cls):
    if cls not in cache:
        txt = subprocess.run([JAVAP, "-s", "-p", "-cp", realjar, cls.replace("/", ".")], capture_output=True, text=True).stdout
        sup = re.search(r"(?:class|interface) \S+(?: extends ([^ {]+))?(?: implements ([^{]+))?", txt)
        parents = []
        if sup:
            if sup.group(1): parents += [x.strip() for x in sup.group(1).split(",")]
            if sup.group(2): parents += [x.strip() for x in sup.group(2).split(",")]
        members, lines = set(), txt.splitlines()
        for i, l in enumerate(lines):
            if "descriptor:" in l:
                name = re.search(r"([\w$<>]+)\(", lines[i-1]) or re.search(r"([\w$]+);$", lines[i-1].strip())
                if name: members.add((name.group(1), l.split("descriptor:")[1].strip()))
        cache[cls] = ([p.replace(".", "/") for p in parents if p != "java.lang.Object"], members)
    return cache[cls]

def declarer(owner, name, desc):
    stack, seen = [owner], set()
    while stack:
        c = stack.pop(0)
        if c in seen: continue
        seen.add(c)
        parents, members = declared(c)
        if (name, desc) in members: return c
        stack += parents
    return None

def to_real_desc(desc):
    return re.sub(r"L([^;]+);", lambda m: "L" + fwd_c.get(m.group(1), m.group(1)) + ";", desc)

rows = set()
for kind, ref in refs(helper):
    if kind == "Class":
        if not ref.startswith(skip): rows.add(("C", ref))
        continue
    owner, rest = ref.split(".", 1)
    if owner.startswith(skip): continue
    name, desc = rest.split(":", 1)
    real_owner = fwd_c.get(owner, owner)
    real_desc = to_real_desc(desc)
    # Candidates: remap rows with this source name/descriptor; keep the one declared in the real hierarchy.
    # (check_uses.py / the round trip in translate-mod.sh verify the result afterwards.)
    cand = [v for k, v in fwd_m.items() if k[1] == name and k[2] == desc and v[2] == real_desc]
    real_member = None
    for ro, rn, rd in cand:
        d = declarer(real_owner, rn, rd)
        if d: real_member = (d, rn, rd); break
    if not real_member:
        print("UNRESOLVED", kind, ref); continue
    decl_src = inv_c.get(real_member[0], real_member[0])
    # Strict check: the remap row for (declaring class, name, descriptor) must point at exactly this member.
    if fwd_m.get((decl_src, name, desc)) != real_member:
        sys.exit(f"USES_MISMATCH {kind} {ref}: row {fwd_m.get((decl_src, name, desc))} != real {real_member}")
    rows.add(("F" if kind == "Fieldref" else "M", owner, name, desc, decl_src))
with open(out, "w") as f:
    for r in sorted(rows): f.write("\t".join(r) + "\n")
print("USES_WRITTEN", len(rows), out)
