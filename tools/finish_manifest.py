"""Build the finished mod.json from the source mod.json and the mod kit's hooks.json.

Follows the rules in BML's docs/sdk/building.md ("The finished mod.json").

  python3 finish_manifest.py <source mod.json> <hooks.json> <out mod.json>
"""
import json
import re
import sys


def target_id(target):
    """Readable ID for a target: '<owner>.<name>' for members, '<class>' for classes."""
    if target["kind"] == "class":
        raw = target["class"]
    else:
        raw = target["owner"] + "." + target["name"]
    raw = raw.replace("/", ".").replace("$", "_")
    return re.sub(r"[^A-Za-z0-9_.-]", "", raw)


def add_target(targets, target):
    """Put a target into the 'targets' map and return its ID (reusing an identical entry)."""
    for tid, existing in targets.items():
        if existing == target:
            return tid
    base = target_id(target)
    tid, n = base, 2
    while tid in targets:
        tid = f"{base}-{n}"
        n += 1
    targets[tid] = target
    return tid


def finish(source, hooks_json):
    mod = json.loads(json.dumps(source))  # deep copy, the source stays untouched
    targets = mod.setdefault("targets", {})
    hooks = mod.setdefault("hooks", [])

    for h in hooks_json.get("hooks", []):
        out = {"id": h["id"], "target": add_target(targets, h["target"]), "transform": h["transform"]}
        if "method_from" in h:
            out["method"] = {"from_target": add_target(targets, h["method_from"])}
            out["body"] = {"template": h["body"]}
        if "match" in h:
            m = h["match"]
            match = {"opcode": m["opcode"]}
            if "target" in m:
                match["target"] = add_target(targets, m["target"])
            else:  # a JDK call: copied as it is
                match.update({k: m[k] for k in ("owner", "name", "descriptor")})
            match["occurrence"] = m.get("occurrence", 1)
            out["match"] = match
        out["bridge"] = h["bridge"]
        hooks.append(out)

    classes = hooks_json.get("hook_classes", [])
    if classes:
        if "helper" in source and source["helper"].get("class") != classes[0]:
            raise ValueError("helper class in mod.json differs from the hook class")
        mod["helper"] = {"jar": "helper.jar", "class": classes[0]}

    if hooks_json.get("fields"):
        raise ValueError("fields are not supported by this small script yet")

    access = hooks_json.get("access", [])
    if access:
        mod["opens"] = sorted({add_target(targets, a) for a in access})
    return mod


def main():
    if len(sys.argv) != 4:
        sys.exit(__doc__)
    with open(sys.argv[1], encoding="utf-8") as f:
        source = json.load(f)
    with open(sys.argv[2], encoding="utf-8") as f:
        hooks_json = json.load(f)
    with open(sys.argv[3], "w", encoding="utf-8") as f:
        json.dump(finish(source, hooks_json), f, indent=2)
        f.write("\n")
    print("FINISH_MANIFEST_OK", sys.argv[3])


if __name__ == "__main__":
    main()
