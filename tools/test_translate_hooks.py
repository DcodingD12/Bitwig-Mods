"""Tests for translate_hooks.py with a tiny hand-made remap.

  python3 -m unittest discover -s tools
"""
import unittest

from translate_hooks import translate

CLASSES = {"Sc": "Hea", "dgs": "dfg"}
MEMBERS = {
    ("M", "Sc", "Heb", "(I)V"): ("Hea", "TtY", "(I)V"),
    ("M", "Sc", "iMb", "(I)V"): ("Hea", "fJR", "(I)V"),
}

HOOKS = {
    "hooks": [{
        "id": "keep-locked-track-height", "transform": "REPLACE_INVOKE",
        "target": {"kind": "method", "owner": "Sc", "name": "Heb", "descriptor": "(I)V"},
        "match": {"opcode": "INVOKEVIRTUAL", "occurrence": 1,
                  "owner": "Sc", "name": "iMb", "descriptor": "(I)V",
                  "target": {"kind": "method", "owner": "Sc", "name": "iMb", "descriptor": "(I)V"}},
        "bridge": {"method": "store", "descriptor": "(LSc;I)V", "args": []},
    }],
    "access": [{"kind": "class", "class": "dgs"}],
    "fields": [],
}


class TranslateHooksTest(unittest.TestCase):
    def test_targets_match_and_bridge_are_renamed(self):
        out = translate(HOOKS, CLASSES, MEMBERS)
        h = out["hooks"][0]
        self.assertEqual(h["target"], {"kind": "method", "owner": "Hea", "name": "TtY", "descriptor": "(I)V"})
        self.assertEqual(h["match"]["target"]["name"], "fJR")
        self.assertEqual((h["match"]["owner"], h["match"]["name"]), ("Hea", "fJR"))
        self.assertEqual(h["bridge"]["descriptor"], "(LHea;I)V")
        self.assertEqual(out["access"], [{"kind": "class", "class": "dfg"}])

    def test_input_is_not_changed(self):
        translate(HOOKS, CLASSES, MEMBERS)
        self.assertEqual(HOOKS["hooks"][0]["target"]["owner"], "Sc")

    def test_unknown_member_stops(self):
        with self.assertRaises(SystemExit):
            translate(HOOKS, CLASSES, {})


if __name__ == "__main__":
    unittest.main()
