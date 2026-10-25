"""Unit tests for MiTool by xboyrex (String-Literal Safety, Self-Healing, Orange->Crimson, and Integrated MiUnlock)."""

import unittest
from mitool import __version__, __repo_url__
from mitool.deps import (
    rewrite_links_in_text,
    replace_orange_with_crimson_in_code,
    _heal_broken_string_literal,
)
from mitool.cli import TOOLS, _transform_subtool_text


class TestIntegratedMiTool(unittest.TestCase):
    def test_exact_5_tools(self):
        self.assertEqual(len(TOOLS), 5)
        self.assertEqual(list(TOOLS.keys()), ["1", "2", "3", "4", "5"])

    def test_rewrite_links_preserves_python_quotes_and_compiles(self):
        # Exact pattern inside miapply/cli.py that caused SyntaxError previously:
        py_code = 'import sys\nprint("\\nHow It Works: https://github.com/offici5l/MiCommunityTool/blob/main/miapply/README.md\\n")\n'
        patched = rewrite_links_in_text(py_code, clickable_osc8=False)
        self.assertEqual(
            patched,
            'import sys\nprint("\\nHow It Works: https://github.com/xboyrex/MiTool\\n")\n',
        )
        # Verify patched code compiles with zero SyntaxError!
        compile(patched, "miapply_cli.py", "exec")

    def test_self_healing_fixes_previously_broken_miapply_line(self):
        broken_code = 'import sys\nprint("\\nHow It Works: https://github.com/xboyrex/MiTool\nx = 1\n'
        healed = _heal_broken_string_literal(broken_code)
        compile(healed, "healed_miapply.py", "exec")
        self.assertIn('print("\\nHow It Works: https://github.com/xboyrex/MiTool\\n")', healed)

    def test_orange_replaced_with_crimson_red(self):
        orig = 'ORANGE = "bold color(208)"\nmsg = "[bold color(208)]1[/] - Browser"\n'
        updated = replace_orange_with_crimson_in_code(orig)
        self.assertNotIn("color(208)", updated)
        self.assertIn("bold #E50914", updated)
        compile(updated, "color_test.py", "exec")

        runtime_str = "\033[38;5;208m1\033[0m - Browser"
        self.assertIn("\033[1;31m", _transform_subtool_text(runtime_str, is_rich=False))

    def test_integrated_miunlock_package_present(self):
        import miunlock.system
        import miunlock.utils
        self.assertEqual(miunlock.system.ORANGE, "bold #E50914")



    def test_gui_module_compiles_and_exports_main(self):
        import mitool.gui
        self.assertTrue(callable(mitool.gui.main))
        self.assertIn("20036", mitool.gui.ERROR_CODE_GUIDE)


if __name__ == "__main__":

    unittest.main()
