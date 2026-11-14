"""Tests for finish_manifest.py, using the example from BML's building.md.

  python3 -m unittest discover -s tools
"""
import unittest

from finish_manifest import finish

HOOKS = {
    "hook_classes": ["com.example.Hooks"],
    "hooks": [{
        "id": "enter-select-first-result",
        "transform": "INSERT_AT_ENTRY",
        "helper_class": "com.example.Hooks",
        "target": {"kind": "method", "owner": "cNC", "name": "eventOccurred", "descriptor": "(Ljava/lang/Object;)V"},
        "bridge": {"method": "ensureFirstSelection", "descriptor": "(LcNC;)V", "args": ["this"]},
    }],
    "access": [
        {"kind": "class", "class": "cNC"},
        {"kind": "field", "owner": "cNC", "name": "cfM", "descriptor": "LcNz;"},
    ],
}


class FinishManifestTest(unittest.TestCase):
    def test_example_from_building_md(self):
        mod = finish({"manifest_version": 2}, HOOKS)
        self.assertEqual(mod["helper"], {"jar": "helper.jar", "class": "com.example.Hooks"})
        self.assertEqual(set(mod["targets"]), {"cNC.eventOccurred", "cNC", "cNC.cfM"})
        self.assertEqual(mod["hooks"][0]["target"], "cNC.eventOccurred")
        self.assertNotIn("helper_class", mod["hooks"][0])
        self.assertEqual(mod["opens"], ["cNC", "cNC.cfM"])

    def test_replace_invoke_gets_match_target(self):
        hooks = {"hook_classes": ["H"], "hooks": [{
            "id": "allow-mouse-drag", "transform": "REPLACE_INVOKE",
            "target": {"kind": "method", "owner": "dSt", "name": "Gef", "descriptor": "(LdSp;)V"},
            "match": {"opcode": "INVOKEVIRTUAL", "occurrence": 1,
                      "target": {"kind": "method", "owner": "dPn", "name": "tWW", "descriptor": "(Z)V"}},
            "bridge": {"method": "allowMouseDrag", "descriptor": "(LdPn;Z)V", "args": []},
        }]}
        mod = finish({}, hooks)
        self.assertEqual(mod["hooks"][0]["match"], {"opcode": "INVOKEVIRTUAL", "target": "dPn.tWW", "occurrence": 1})
        self.assertNotIn("opens", mod)

    def test_same_id_gets_suffix(self):
        hooks = {"hooks": [], "access": [
            {"kind": "method", "owner": "a", "name": "b", "descriptor": "()V"},
            {"kind": "method", "owner": "a", "name": "b", "descriptor": "(I)V"},
        ]}
        mod = finish({}, hooks)
        self.assertEqual(mod["opens"], ["a.b", "a.b-2"])

    def test_empty_hooks_still_has_targets_and_hooks(self):
        mod = finish({"mod": {"id": "x"}}, {"hooks": [], "access": []})
        self.assertEqual(mod["targets"], {})
        self.assertEqual(mod["hooks"], [])


if __name__ == "__main__":
    unittest.main()
