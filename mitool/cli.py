"""
MiTool by xboyrex (v2.0.0) — Main CLI & Boxed Terminal Interface.

- Big Block `XBOYREX` Double-Edge Crimson Box UI + Rounded `Execution Pipeline` Box
- Contains ONLY the exact 5 original MiTool operations (`[01]` to `[05]` + `[Q ]` Quit)
- Overrides ALL Orange colors (`color(208)`, `#FF6900`, `\033[33m`, `\033[38;5;208m`) in
  sub-tools (`migate`, `miunlock`, `miflash`, `miasst`, `fcetool`, `miapply`) with
  XBOYREX Crimson Red (`bold #E50914` / `\033[1;31m`)!
- Rewrites any `How It Works:` link to clickable `https://github.com/xboyrex/MiTool`.
"""

import builtins
import importlib.metadata
import re
import shutil
import subprocess
import sys
from rich.console import Console
from rich.panel import Panel
from rich.table import Table
from rich.text import Text
from rich import box

from mitool import __version__, __repo_url__, deps
from mitool.net_engine import install_smart_dns_hook

VERSION = __version__

console = Console(highlight=False)

RED = "#E50914"
CRIMSON = "bold #E50914"
WHITE = "bold #F8FAFC"
GRAPHITE = "#64748B"
GREEN = "bold #10B981"
DIM = "dim #94A3B8"

# Exact 5 original MiTool operations
TOOLS = {
    "1": ("Unlock Bootloader", "miunlock"),
    "2": ("Flash Fastboot ROM.tgz", "miflash"),
    "3": ("Mi Assistant", "miasst"),
    "4": ("Firmware Content Extractor", "fcetool"),
    "5": ("Apply Unlock Permission", "miapply"),
}

BIG_XBOYREX_LOGO = (
    "██╗  ██╗██████╗  ██████╗ ██╗   ██╗██████╗ ███████╗██╗  ██╗\n"
    "╚██╗██╔╝██╔══██╗██╔═══██╗╚██╗ ██╔╝██╔══██╗██╔════╝╚██╗██╔╝\n"
    " ╚███╔╝ ██████╔╝██║   ██║ ╚████╔╝ ██████╔╝█████╗   ╚███╔╝ \n"
    " ██╔██╗ ██╔══██╗██║   ██║  ╚██╔╝  ██╔══██╗██╔══╝   ██╔██╗ \n"
    "██╔╝ ██╗██████╔╝╚██████╔╝   ██║   ██║  ██║███████╗██╔╝ ██╗\n"
    "╚═╝  ╚═╝╚═════╝  ╚═════╝    ╚═╝   ╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝"
)


def print_header():
    header = Text()
    header.append(BIG_XBOYREX_LOGO + "\n", style=CRIMSON)
    header.append("MI TOOL BY XBOYREX", style=WHITE)
    header.append(f"  v{VERSION}\n", style=RED)
    header.append("XIAOMI BOOTLOADER & FLASHING UTILITY  •  Cross-Platform • Python • Fastboot\n", style=DIM)
    header.append("Developed by xboyrex · Independent Community Edition\n", style=GRAPHITE)
    header.append("GitHub: ", style=DIM)
    header.append(
        __repo_url__,
        style=f"underline {RED} link {__repo_url__}",
    )

    panel = Panel(
        header,
        border_style=RED,
        box=box.DOUBLE_EDGE,
        padding=(1, 2),
        title="[bold #F8FAFC][ XBOYREX EDITION ][/]",
        subtitle="[#64748B]MIUI & HyperOS Bootloader & Flashing Engine[/]",
    )
    console.print(panel)


def print_menu():
    tbl = Table(box=box.SIMPLE, show_header=False, padding=(0, 1))
    tbl.add_column("Stage", style=CRIMSON, no_wrap=True)
    tbl.add_column("Title", style=WHITE)

    for key, (desc, _) in TOOLS.items():
        tbl.add_row(f"[0{key}]", desc)

    tbl.add_row("", "")
    if not deps._is_termux():
        tbl.add_row("[G ]", "Launch Desktop GUI (PC Window)")
    tbl.add_row("[Q ]", "[dim #94A3B8]Quit[/]")

    console.print(
        Panel(
            tbl,
            title="[bold #E50914]Execution Pipeline[/]",
            subtitle=f"[link={__repo_url__}][#64748B]How It Works: {__repo_url__}[/][/link]",
            border_style=GRAPHITE,
            box=box.ROUNDED,
            padding=(0, 2),
        )
    )


def _transform_subtool_text(s: str, is_rich: bool = True) -> str:
    """
    1. Rewrites any upstream URL / `How It Works:` to `https://github.com/xboyrex/MiTool`.
    2. Replaces all Orange markup (`color(208)`, `#FF6900`, `orange1`, `dark_orange`, ANSI 208)
       with Crimson Red (`#E50914` / `\033[1;31m`).
    """
    out = deps.rewrite_links_in_text(s, clickable_osc8=(not is_rich))
    out = out.replace("color(208)", "#E50914")
    out = out.replace("#FF6900", "#E50914")
    out = out.replace("#ff6900", "#E50914")
    out = out.replace("\033[38;5;208m", "\033[1;31m")
    out = out.replace("\033[38;5;214m", "\033[1;31m")
    out = out.replace("\033[33m", "\033[1;31m")
    out = out.replace("\033[1;33m", "\033[1;31m")
    if is_rich and __repo_url__ in out and f"link={__repo_url__}" not in out:
        out = out.replace(
            __repo_url__,
            f"[underline bold #E50914 link={__repo_url__}]{__repo_url__}[/]",
        )
    return out


def _install_link_color_and_dns_interceptors():
    """
    Activate Smart DoH DNS, override `migate.config.ORANGE` in memory, and intercept
    Rich `Console.print`, `Console.input`, `builtins.print`, and `builtins.input`
    so all sub-tools render in Crimson Red instead of Orange and all links are clickable!
    """
    install_smart_dns_hook()

    # Override migate.config.ORANGE in memory if migate is installed
    try:
        import migate.config
        migate.config.ORANGE = "bold #E50914"
    except Exception:
        pass

    try:
        import rich.console
        if not getattr(rich.console.Console.print, "_xboyrex_patched", False):
            _orig_rich_print = rich.console.Console.print

            def _patched_rich_print(self, *args, **kwargs):
                new_args = [
                    _transform_subtool_text(a, is_rich=True) if isinstance(a, str) else a
                    for a in args
                ]
                return _orig_rich_print(self, *new_args, **kwargs)

            _patched_rich_print._xboyrex_patched = True
            rich.console.Console.print = _patched_rich_print

        if not getattr(rich.console.Console.input, "_xboyrex_patched", False):
            _orig_rich_input = rich.console.Console.input

            def _patched_rich_input(self, prompt="", *args, **kwargs):
                if isinstance(prompt, str):
                    prompt = _transform_subtool_text(prompt, is_rich=True)
                return _orig_rich_input(self, prompt, *args, **kwargs)

            _patched_rich_input._xboyrex_patched = True
            rich.console.Console.input = _patched_rich_input
    except Exception:
        pass

    if not getattr(builtins.print, "_xboyrex_patched", False):
        _orig_print = builtins.print

        def _patched_print(*args, **kwargs):
            new_args = [
                _transform_subtool_text(a, is_rich=False) if isinstance(a, str) else a
                for a in args
            ]
            return _orig_print(*new_args, **kwargs)

        _patched_print._xboyrex_patched = True
        builtins.print = _patched_print

    if not getattr(builtins.input, "_xboyrex_patched", False):
        _orig_input = builtins.input

        def _patched_input(prompt=""):
            if isinstance(prompt, str):
                prompt = _transform_subtool_text(prompt, is_rich=False)
            return _orig_input(prompt)

        _patched_input._xboyrex_patched = True
        builtins.input = _patched_input


def launch_subtool(cmd: str) -> int:
    """
    Bulletproof launcher for `miunlock`, `miflash`, `miasst`, `fcetool`, and `miapply`:
    - If `cmd == "miunlock"`, can run integrated `miunlock.cli:main` directly!
    - Checks & heals any syntax-corrupted files in site-packages (`deps.patch_subtool_links()`).
    - Overrides all Orange colors with Crimson Red (`#E50914`).
    - Executes via `importlib.metadata.entry_points` with automatic self-reinstall recovery if
      a package was previously damaged on disk.
    """
    deps.patch_subtool_links()
    _install_link_color_and_dns_interceptors()

    if cmd == "miunlock":
        try:
            from miunlock.cli import main as miunlock_main
            sys.argv = ["miunlock"]
            miunlock_main()
            return 0
        except SystemExit as e:
            return int(e.code) if isinstance(e.code, int) else 0
        except KeyboardInterrupt:
            console.print(f"\n[{CRIMSON}]Cancelled[/]")
            return 0
        except Exception:
            pass

    eps = importlib.metadata.entry_points(group="console_scripts")
    ep_matches = [ep for ep in eps if ep.name == cmd]

    if not ep_matches and not shutil.which(cmd):
        console.print(f"  [{DIM}]Installing '{cmd}' via pip...[/]")
        subprocess.run([sys.executable, "-m", "pip", "install", "-U", cmd], check=False)
        deps.patch_subtool_links()
        eps = importlib.metadata.entry_points(group="console_scripts")
        ep_matches = [ep for ep in eps if ep.name == cmd]

    if ep_matches:
        try:
            sys.argv = [cmd]
            func = ep_matches[0].load()
            res = func()
            return int(res) if isinstance(res, int) else 0
        except SyntaxError:
            # Self-healing recovery: if the installed package had a broken .py file on disk,
            # force-reinstall a fresh copy from PyPI, patch safely, and run!
            console.print(f"  [{DIM}]Repairing '{cmd}' package files...[/]")
            subprocess.run(
                [sys.executable, "-m", "pip", "install", "--force-reinstall", "--no-deps", "-q", cmd],
                check=False,
            )
            deps.patch_subtool_links()
            # Clear cached broken modules from sys.modules
            for mod_name in list(sys.modules.keys()):
                if mod_name == cmd or mod_name.startswith(f"{cmd}."):
                    sys.modules.pop(mod_name, None)
            func = ep_matches[0].load()
            res = func()
            return int(res) if isinstance(res, int) else 0
        except SystemExit as e:
            return int(e.code) if isinstance(e.code, int) else 0
        except KeyboardInterrupt:
            console.print(f"\n[{CRIMSON}]Cancelled[/]")
            return 0

    return subprocess.run(cmd, shell=True).returncode


def get_choice():
    if len(sys.argv) > 1:
        raw = sys.argv[1].strip().lower()
        return raw.lstrip("0") or raw
    try:
        raw = console.input(f"\n[{CRIMSON}]▌[/] [{WHITE}]Choice[/] [{DIM}]›[/] ").strip().lower()
        return raw.lstrip("0") or raw
    except (KeyboardInterrupt, EOFError):
        console.print(f"\n[{CRIMSON}]Cancelled[/]")
        sys.exit(0)


def main():
    deps.ensure_deps()
    print_header()
    print_menu()

    choice = get_choice()

    if choice in ("q", "quit", "exit"):
        sys.exit(0)
    if choice in ("g", "gui", "--gui"):
        from mitool.gui import main as gui_main
        gui_main()
        sys.exit(0)

    if choice not in TOOLS:
        console.print(f"[{CRIMSON}]✗[/] [{WHITE}]Invalid choice:[/] {choice}")
        sys.exit(1)

    desc, cmd = TOOLS[choice]
    console.print(f"\n[{CRIMSON}][0{choice}][/{CRIMSON}] [{WHITE}]{desc}[/{WHITE}]\n")
    launch_subtool(cmd)


if __name__ == "__main__":
    main()
