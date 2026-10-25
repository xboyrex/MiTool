import os
import subprocess
import tempfile
import urllib.request
from pathlib import Path

from miunlock.system import console, make_executable, WHITE, RED

PREFIX = os.environ.get("PREFIX", "/data/data/com.termux/files/usr")


def install_termux_adb():
    gpg_path = f"{PREFIX}/etc/apt/trusted.gpg.d/nohajc.gpg"
    os.makedirs(os.path.dirname(gpg_path), exist_ok=True)

    with console.status(f"[{WHITE}]Installing termux-adb (first time only) ...[/]"):
        with urllib.request.urlopen("https://nohajc.github.io/nohajc.gpg", timeout=30) as resp:
            with open(gpg_path, "wb") as f:
                f.write(resp.read())

        install_url = "https://raw.githubusercontent.com/nohajc/termux-adb/master/install.sh"
        with urllib.request.urlopen(install_url, timeout=30) as resp:
            script = resp.read().decode("utf-8")

        with tempfile.NamedTemporaryFile(mode="w", suffix=".sh", delete=False) as f:
            f.write(script)
            tmp_path = f.name

        try:
            yes_proc = subprocess.Popen(["yes"], stdout=subprocess.PIPE)
            proc = subprocess.run(["bash", tmp_path], stdin=yes_proc.stdout, capture_output=True, text=True)
            yes_proc.kill()
        finally:
            os.unlink(tmp_path)

    if not os.path.isfile(f"{PREFIX}/bin/termux-fastboot"):
        console.print(f"\n[{RED}]✗ termux-adb installation failed:\n{proc.stderr}\n[/]")
        raise SystemExit(1)


def setup_fastboot():
    config_file = Path.home() / ".termux" / "termux.properties"
    config_file.parent.mkdir(parents=True, exist_ok=True)
    existing_content = config_file.read_text(encoding="utf-8") if config_file.exists() else ""
    if "allow-external-apps = true" not in existing_content:
        with open(config_file, "a", encoding="utf-8") as f:
            f.write("\nallow-external-apps = true\n")
        subprocess.run(["termux-reload-settings"], check=False)

    result = subprocess.run(
        ["cmd", "package", "list", "packages", "--user", "0", "com.termux.api"],
        capture_output=True,
        text=True,
    )
    if "package:com.termux.api" not in result.stdout:
        console.print(
            f"\n[{RED}]✗ com.termux.api is not installed!\n"
            "Download it from https://github.com/termux/termux-api/releases/latest and rerun the tool.\n[/]"
        )
        raise SystemExit(1)

    termux_fastboot_path = f"{PREFIX}/bin/termux-fastboot"
    if not os.path.isfile(termux_fastboot_path):
        install_termux_adb()

    fastboot_path = f"{PREFIX}/bin/fastboot"
    if os.path.islink(fastboot_path) or os.path.exists(fastboot_path):
        os.remove(fastboot_path)
    os.symlink(termux_fastboot_path, fastboot_path)
    make_executable(fastboot_path)
    return fastboot_path