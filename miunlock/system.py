import builtins
import json
import os
import platform
import socket
import stat
import urllib.request

try:
    from migate.config import console, WHITE, DIM, GREEN
    import migate.config as _migate_cfg
    _migate_cfg.ORANGE = "bold #E50914"
    _migate_cfg.RED = "bold #E50914"
except ImportError:
    from rich.console import Console
    console = Console(highlight=False)
    WHITE = "bold #F8FAFC"
    DIM = "dim #94A3B8"
    GREEN = "bold #10B981"

from rich.panel import Panel
from rich.table import Table
from rich.text import Text
from rich import box

# Override Orange with XBOYREX Crimson Red (#E50914) everywhere
RED = "bold #E50914"
ORANGE = "bold #E50914"
CRIMSON = "bold #E50914"
GRAPHITE = "#64748B"

VERSION = "2.0.0"
REPO_URL = "https://github.com/xboyrex/MiTool"

SYSTEM = platform.system()
IS_TERMUX = "com.termux" in os.environ.get("PREFIX", "") or os.path.isdir("/data/data/com.termux/files/usr")

BIG_XBOYREX_LOGO = (
    "██╗  ██╗██████╗  ██████╗ ██╗   ██╗██████╗ ███████╗██╗  ██╗\n"
    "╚██╗██╔╝██╔══██╗██╔═══██╗╚██╗ ██╔╝██╔══██╗██╔════╝╚██╗██╔╝\n"
    " ╚███╔╝ ██████╔╝██║   ██║ ╚████╔╝ ██████╔╝█████╗   ╚███╔╝ \n"
    " ██╔██╗ ██╔══██╗██║   ██║  ╚██╔╝  ██╔══██╗██╔══╝   ██╔██╗ \n"
    "██╔╝ ██╗██████╔╝╚██████╔╝   ██║   ██║  ██║███████╗██╔╝ ██╗\n"
    "╚═╝  ╚═╝╚═════╝  ╚═════╝    ╚═╝   ╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝"
)

# Smart DoH DNS Shield (Cloudflare 1.1.1.1 & Google 8.8.8.8)
_DNS_CACHE = {}


def _resolve_doh(host: str) -> str:
    if host in _DNS_CACHE:
        return _DNS_CACHE[host]
    for url_t in (
        f"https://1.1.1.1/dns-query?name={host}&type=A",
        f"https://8.8.8.8/resolve?name={host}&type=A",
    ):
        try:
            req = urllib.request.Request(url_t, headers={"Accept": "application/dns-json"})
            with urllib.request.urlopen(req, timeout=2.0) as resp:
                data = json.loads(resp.read().decode("utf-8"))
                for ans in data.get("Answer", []):
                    if ans.get("type") == 1 and ans.get("data"):
                        _DNS_CACHE[host] = ans["data"]
                        return ans["data"]
        except Exception:
            continue
    return ""


def install_smart_dns_and_color_hooks():
    orig_getaddrinfo = socket.getaddrinfo

    def _smart_getaddrinfo(host, port, family=0, type=0, proto=0, flags=0):
        try:
            return orig_getaddrinfo(host, port, family, type, proto, flags)
        except socket.gaierror:
            if isinstance(host, str):
                ip = _resolve_doh(host)
                if ip:
                    return orig_getaddrinfo(ip, port, family, type, proto, flags)
            raise

    if getattr(socket.getaddrinfo, "__name__", "") != "_smart_getaddrinfo":
        socket.getaddrinfo = _smart_getaddrinfo

    # Intercept Rich Console.print & input so migate's orange prompts turn Crimson Red (#E50914)
    try:
        import rich.console
        if not getattr(rich.console.Console.print, "_xboyrex_mu_patched", False):
            _orig_p = rich.console.Console.print

            def _patched_p(self, *args, **kwargs):
                new_args = [
                    a.replace("color(208)", "#E50914").replace("#FF6900", "#E50914")
                    if isinstance(a, str) else a
                    for a in args
                ]
                return _orig_p(self, *new_args, **kwargs)

            _patched_p._xboyrex_mu_patched = True
            rich.console.Console.print = _patched_p

        if not getattr(rich.console.Console.input, "_xboyrex_mu_patched", False):
            _orig_in = rich.console.Console.input

            def _patched_in(self, prompt="", *args, **kwargs):
                if isinstance(prompt, str):
                    prompt = prompt.replace("color(208)", "#E50914").replace("#FF6900", "#E50914")
                return _orig_in(self, prompt, *args, **kwargs)

            _patched_in._xboyrex_mu_patched = True
            rich.console.Console.input = _patched_in
    except Exception:
        pass


def render_xboyrex_startup():
    header = Text()
    header.append(BIG_XBOYREX_LOGO + "\n", style=CRIMSON)
    header.append("MI UNLOCK TOOL BY XBOYREX", style=WHITE)
    header.append(f"  v{VERSION}\n", style="#E50914")
    header.append("XIAOMI BOOTLOADER UTILITY  •  Cross-Platform • Python • Fastboot\n", style=DIM)
    header.append("Developed by xboyrex (Mayank Droid) · Independent Community Edition\n", style=GRAPHITE)
    header.append("GitHub: ", style=DIM)
    header.append(REPO_URL, style=f"underline #E50914 link {REPO_URL}")

    console.print(
        Panel(
            header,
            border_style="#E50914",
            box=box.DOUBLE_EDGE,
            padding=(1, 2),
            title="[bold #F8FAFC][ XBOYREX EDITION ][/]",
            subtitle="[#64748B]MIUI & HyperOS Bootloader Unlock Engine[/]",
        )
    )

    tbl = Table(box=box.SIMPLE, show_header=False, padding=(0, 1))
    tbl.add_column("Stage", style=CRIMSON, no_wrap=True)
    tbl.add_column("Title", style=WHITE)
    tbl.add_row("[01]", "Environment Check")
    tbl.add_row("[02]", "Xiaomi Account Login")
    tbl.add_row("[03]", "Device Detection")
    tbl.add_row("[04]", "Unlock Eligibility")
    tbl.add_row("[05]", "Review & Confirm")

    console.print(
        Panel(
            tbl,
            title="[bold #E50914]Execution Pipeline[/]",
            subtitle=f"[link={REPO_URL}][#64748B]How It Works: {REPO_URL}[/][/link]",
            border_style=GRAPHITE,
            box=box.ROUNDED,
            padding=(0, 2),
        )
    )


def make_executable(path):
    if SYSTEM not in ("Linux", "Darwin", "Android"):
        return
    if not os.path.exists(path):
        return
    st = os.stat(path)
    if os.getuid() == st.st_uid:
        x_bit = stat.S_IXUSR
    elif os.getgid() == st.st_gid:
        x_bit = stat.S_IXGRP
    else:
        x_bit = stat.S_IXOTH
    if not (st.st_mode & x_bit):
        os.chmod(path, st.st_mode | x_bit)
