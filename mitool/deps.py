import os
import platform
import re
import shutil
import site
import subprocess
import sys
from pathlib import Path
from typing import List

from mitool import __repo_url__

_MARKER = os.path.expanduser("~/.mitool_deps_ok")
PREFIX = os.environ.get("PREFIX", "/data/data/com.termux/files/usr")

# Strict URL regex that NEVER matches quotes (" or '), parentheses ), brackets ], or backslashes \
UPSTREAM_URL_PATTERNS = [
    r"https?://github\.com/MiForge/[A-Za-z0-9_.-]+(?:/blob/main/[A-Za-z0-9_./-]+)?",
    r"https?://github\.com/offici5l/[A-Za-z0-9_.-]+(?:/blob/main/[A-Za-z0-9_./-]+)?",
    r"https?://offici5l\.github\.io(?:/[A-Za-z0-9_./-]*)?",
    r"https?://miforge\.github\.io(?:/[A-Za-z0-9_./-]*)?",
    r"https?://xboyrex\.github\.io/mayank-droid/?",
]

COMBINED_URL_REGEX = re.compile("|".join(f"({p})" for p in UPSTREAM_URL_PATTERNS))

# Safe regex for `How It Works: <url>` — stops strictly before quotes, whitespace, backslashes, or parens!
HOW_IT_WORKS_REGEX = re.compile(
    r"(How\s+It\s+Works\s*:\s*)(https?://[^\s\"'<>)\]\r\n\\]+)",
    re.IGNORECASE,
)


def _is_termux():
    return os.path.exists("/data/data/com.termux")


PKGS = {
    "apt-get": "libusb-1.0-0",
    "dnf": "libusb1",
    "yum": "libusb1",
    "pacman": "libusb",
    "zypper": "libusb-1_0-0",
}

UDEV_PATH = "/etc/udev/rules.d/51-mitool.rules"
UDEV_RULES = (
    'SUBSYSTEM=="usb", ATTR{idVendor}=="2717", MODE="0666", GROUP="plugdev"\n'
    'SUBSYSTEM=="usb", ATTR{idVendor}=="05c6", MODE="0666", GROUP="plugdev"\n'
    'SUBSYSTEM=="usb", ATTR{idVendor}=="18d1", MODE="0666", GROUP="plugdev"\n'
)


def rewrite_links_in_text(text: str, clickable_osc8: bool = False) -> str:
    """
    Safely replace any upstream URL or `How It Works: <url>` with `https://github.com/xboyrex/MiTool`
    without EVER touching surrounding quotes `"` / `'`, escape sequences `\n`, or parentheses `)`.
    """
    target = __repo_url__
    if clickable_osc8:
        replacement = f"\033]8;;{target}\033\\{target}\033]8;;\033\\"
    else:
        replacement = target

    out = COMBINED_URL_REGEX.sub(lambda _m: replacement, text)
    out = HOW_IT_WORKS_REGEX.sub(lambda m: f"{m.group(1)}{replacement}", out)
    return out


def replace_orange_with_crimson_in_code(code: str) -> str:
    """
    Replace orange theme definitions in sub-tools (`migate`, `miunlock`, `miflash`,
    `miasst`, `fcetool`, `miapply`) with XBOYREX Crimson Red (`bold #E50914`).
    """
    out = code
    # Rich color(208) / #FF6900 / dark_orange / orange1 replacements
    out = re.sub(r'ORANGE\s*=\s*["\']bold color\(208\)["\']', 'ORANGE = "bold #E50914"', out)
    out = re.sub(r'ORANGE\s*=\s*["\']color\(208\)["\']', 'ORANGE = "#E50914"', out)
    out = re.sub(r'ORANGE\s*=\s*["\'][^"\']*orange[^"\']*["\']', 'ORANGE = "bold #E50914"', out, flags=re.IGNORECASE)
    out = out.replace("bold color(208)", "bold #E50914")
    out = out.replace("color(208)", "#E50914")
    out = out.replace("#FF6900", "#E50914")
    out = out.replace("#ff6900", "#E50914")
    # ANSI 256-color orange (208 / 214) -> bright red \033[1;31m
    out = out.replace("\\033[38;5;208m", "\\033[1;31m")
    out = out.replace("\\033[38;5;214m", "\\033[1;31m")
    out = out.replace("\033[38;5;208m", "\033[1;31m")
    out = out.replace("\033[38;5;214m", "\033[1;31m")
    return out


def _heal_broken_string_literal(code: str) -> str:
    """
    Self-healing fixer: if an earlier regex accidentally stripped `\\n")` at the end
    of `print("\\nHow It Works: https://github.com/xboyrex/MiTool` in `miapply/cli.py`,
    restore the closing `\\n")` immediately!
    """
    fixed_lines = []
    for line in code.splitlines():
        stripped = line.rstrip()
        if "How It Works:" in stripped and stripped.endswith(__repo_url__):
            if "console.print(" in stripped or "print(" in stripped:
                q_char = '"' if '"' in stripped else "'"
                line = f'{stripped}\\n"{")"}' if q_char == '"' else f"{stripped}\\n'{')'}"
        fixed_lines.append(line)
    return "\n".join(fixed_lines) + ("\n" if code.endswith("\n") else "")


def patch_subtool_links() -> int:
    """
    Scan installed `miapply`, `miunlock`, `miflash`, `miasst`, `fcetool`, `migate`,
    and `micommunity` packages in site-packages:
    1. Self-heals any file that currently has a `SyntaxError` (and reinstalls the package via pip if needed).
    2. Safely rewrites upstream URLs to `https://github.com/xboyrex/MiTool`.
    3. Replaces Orange color constants (`color(208)`, `#FF6900`) with Crimson Red (`#E50914`).
    4. Verifies valid Python syntax via `compile()` BEFORE saving!
    """
    patched = 0
    target_pkgs = ("miapply", "miunlock", "miflash", "miasst", "fcetool", "migate", "micommunity")

    search_dirs: List[Path] = []
    try:
        for sp in site.getsitepackages():
            search_dirs.append(Path(sp))
    except Exception:
        pass
    try:
        usp = site.getusersitepackages()
        if usp:
            search_dirs.append(Path(usp))
    except Exception:
        pass
    for p in sys.path:
        if p and "site-packages" in p:
            search_dirs.append(Path(p))

    seen = set()
    for base in search_dirs:
        if not base.is_dir():
            continue
        for pkg in target_pkgs:
            pkg_dir = base / pkg
            if not pkg_dir.is_dir():
                continue
            for py_file in pkg_dir.rglob("*.py"):
                if py_file in seen:
                    continue
                seen.add(py_file)
                try:
                    orig = py_file.read_text(encoding="utf-8")
                except Exception:
                    continue

                candidate = orig
                # Step 1: Check if the file is currently broken from an earlier run
                try:
                    compile(candidate, str(py_file), "exec")
                except SyntaxError:
                    candidate = _heal_broken_string_literal(candidate)
                    try:
                        compile(candidate, str(py_file), "exec")
                    except SyntaxError:
                        # Reinstall clean package from PyPI if healing couldn't parse it
                        subprocess.run(
                            [sys.executable, "-m", "pip", "install", "--force-reinstall", "--no-deps", "-q", pkg],
                            check=False,
                        )
                        try:
                            orig = py_file.read_text(encoding="utf-8")
                            candidate = orig
                        except Exception:
                            continue

                # Step 2: Apply safe URL rewrite and Orange -> Crimson Red color replacement
                updated = rewrite_links_in_text(candidate, clickable_osc8=False)
                updated = replace_orange_with_crimson_in_code(updated)

                # Step 3: Strictly verify syntax with `compile()` before writing to disk!
                if updated != orig:
                    try:
                        compile(updated, str(py_file), "exec")
                        py_file.write_text(updated, encoding="utf-8")
                        patched += 1
                    except SyntaxError:
                        # Never write a file that fails syntax compilation
                        pass

    return patched


def _run(cmd):
    if not shutil.which(cmd[0]):
        return False
    print(f"Installing missing dependency: {' '.join(cmd)}")
    subprocess.run(cmd)
    return True


def _termux_api_app_installed():
    result = subprocess.run(
        ["cmd", "package", "list", "packages", "--user", "0", "com.termux.api"],
        capture_output=True,
        text=True,
    )
    return "package:com.termux.api" in result.stdout


def _ensure_termux():
    ok = True

    if not _termux_api_app_installed():
        print(
            "Termux:API app is not installed.\n"
            "Download it from https://github.com/termux/termux-api/releases/latest and rerun."
        )
        ok = False

    lib_path = f"{PREFIX}/lib/libusb-1.0.so"
    if not os.path.exists(lib_path):
        _run(["pkg", "install", "-y", "libusb"])
        if not os.path.exists(lib_path):
            print("Could not install libusb. Try manually: pkg install libusb")
            ok = False

    if not shutil.which("termux-usb"):
        _run(["pkg", "install", "-y", "termux-api"])
        if not shutil.which("termux-usb"):
            print("Could not install termux-api. Try manually: pkg install termux-api")
            ok = False

    return ok


def _windows_backend():
    try:
        import usb.backend.libusb1
        import libusb_package
        return usb.backend.libusb1.get_backend(find_library=libusb_package.find_library)
    except ImportError:
        return None


def _ensure_windows():
    if _windows_backend() is not None:
        return True

    print("libusb backend unavailable, attempting to reinstall libusb_package...")
    subprocess.run([sys.executable, "-m", "pip", "install", "--force-reinstall", "-q", "libusb_package"])

    if _windows_backend() is None:
        print("Still unavailable. Try manually: pip install --force-reinstall libusb_package, then restart mitool.")
        return False
    return True


def _install_libusb_unix():
    if platform.system() == "Darwin":
        if not shutil.which("brew"):
            print("Homebrew not found. Install libusb manually: brew install libusb")
            return False
        return _run(["brew", "install", "libusb"])

    for mgr, pkg in PKGS.items():
        if not shutil.which(mgr):
            continue
        cmd = [mgr, "-S", "--noconfirm", pkg] if mgr == "pacman" else [mgr, "install", "-y", pkg]
        if os.geteuid() != 0:
            if not shutil.which("sudo"):
                print(f"Need root to install {pkg}. Try manually: sudo {' '.join(cmd)}")
                return False
            cmd = ["sudo"] + cmd
        return _run(cmd)

    print("No supported package manager found. Install libusb manually for your distro.")
    return False


def _ensure_udev():
    if platform.system() != "Linux" or _is_termux():
        return
    if os.geteuid() != 0 and not shutil.which("sudo"):
        return

    try:
        if open(UDEV_PATH).read() == UDEV_RULES:
            return
    except OSError:
        pass

    prefix = [] if os.geteuid() == 0 else ["sudo"]
    print("Writing udev rules ->", UDEV_PATH)
    p = subprocess.Popen(prefix + ["tee", UDEV_PATH], stdin=subprocess.PIPE, text=True, stdout=subprocess.DEVNULL)
    p.communicate(UDEV_RULES)
    subprocess.run(prefix + ["udevadm", "control", "--reload-rules"])
    subprocess.run(prefix + ["udevadm", "trigger"])


def _ensure_unix():
    import usb.backend.libusb1
    if not usb.backend.libusb1.get_backend():
        if not _install_libusb_unix():
            return False
        if not usb.backend.libusb1.get_backend():
            print("Installed but still not detected. Try manually and rerun mitool.")
            return False

    _ensure_udev()
    return True


def ensure_deps():
    # Always run fast AST-safe link & color healing on sub-tools
    patch_subtool_links()

    if os.path.exists(_MARKER):
        return

    if platform.system() == "Windows":
        ok = _ensure_windows()
    elif _is_termux():
        ok = _ensure_termux()
    else:
        ok = _ensure_unix()

    if ok:
        open(_MARKER, "w").close()
