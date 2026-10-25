"""
XBOYREX MiTool + MiUnlockTool Unified Desktop GUI for Windows, Linux, and macOS.
Crafted in the signature XBOYREX Obsidian Dark (`#080A0F`) & Crimson Red (`#E50914`) theme.

Features:
- Launch & interact with all 5 core operations (`Unlock Bootloader`, `Flash Fastboot ROM.tgz`,
  `Mi Assistant`, `Firmware Content Extractor`, `Apply Unlock Permission`) with a real-time
  streaming terminal console and interactive input box!
- Direct Fastboot & ADB Device Inspector (reads `product`, `unlocked`, `anti`, `serialno`,
  `deviceToken`, and ADB properties in 1 click).
- Built-in Xiaomi Error Code Resolver (`10000`..`30002`) & Smart DoH DNS (`1.1.1.1` / `8.8.8.8`) Latency Tester.
- Official Quick Links (`Telegram`, `YouTube`, `GitHub`, `Website`).
"""

import os
import platform
import queue
import shutil
import subprocess
import sys
import threading
import time
import webbrowser
from pathlib import Path
try:
    import tkinter as tk
    from tkinter import filedialog, messagebox
    _HAS_TK = True
except ImportError:
    tk = None
    filedialog = None
    messagebox = None
    _HAS_TK = False

# Auto-install customtkinter on desktop if missing, with clean fallback to standard tkinter
try:
    import customtkinter as ctk
    _HAS_CTK = True
except ImportError:
    _HAS_CTK = False
    ctk = None

from mitool import __version__, __repo_url__, deps
from mitool.net_engine import install_smart_dns_hook, resolve_host_smart

BG_DARK = "#080A0F"
CARD_BG = "#0E121B"
CARD_ELEVATED = "#151B28"
BORDER_COLOR = "#262E42"
CRIMSON = "#E50914"
CRIMSON_HOVER = "#B8060E"
TEXT_WHITE = "#F8FAFC"
TEXT_MUTED = "#94A3B8"
GREEN_OK = "#10B981"

TELEGRAM_URL = "https://t.me/MayankDroid7"
YOUTUBE_URL = "https://youtube.com/@mayankdroid"
GITHUB_URL = "https://github.com/xboyrex/MiTool"
WEBSITE_URL = "https://xboyrex.github.io/mayank-droid/"

OPERATIONS = [
    ("01", "Unlock Bootloader (miunlock)", "miunlock", "Retrieve Xiaomi encryptData(token) & unlock bootloader in Fastboot mode"),
    ("02", "Flash Fastboot ROM.tgz (miflash)", "miflash", "Flash official MIUI / HyperOS .tgz Fastboot ROM packages"),
    ("03", "Mi Assistant Mode (miasst)", "miasst", "Read Info, Sideload Recovery ROMs, Format Data & Reboot without unlocking"),
    ("04", "Firmware Content Extractor (fcetool)", "fcetool", "Extract boot.img / init_boot.img directly from remote ROM URLs Over-The-Air"),
    ("05", "Apply Unlock Permission (miapply)", "miapply", "Submit HyperOS 00:00 Beijing Time (GMT+8) unlock quota request"),
]

ERROR_CODE_GUIDE = {
    "10000": "Request parameter error — Invalid deviceToken or product codename. Reconnect in Fastboot mode (not fastbootd) and retry.",
    "10013": "Device not activated / activated < 7 days — Disable custom DNS, insert SIM on mobile data, enable Call/Message sync in Xiaomi Cloud, and re-bind in Mi Unlock Status.",
    "10023": "Annual quota reached — Xiaomi allows unlocking 1 device per year per account. Use another Xiaomi account.",
    "20030": "Monthly token quota reached — An encryptData token was already retrieved this month. Wait until the 1st of next month.",
    "20031": "Account not bound in Mi Unlock Status — Go to Settings > Developer options > Mi Unlock status > Add account and device (using mobile data). Or select matching dataCenterZone.",
    "20033": "Account restricted — Enable Find Device in Xiaomi Account settings or try a different Xiaomi account.",
    "20035": "Outdated clientVersion — Update MiTool / MiUnlockTool to the latest version.",
    "20036": "Mandatory waiting period (e.g., 72 / 168 hours) — Wait until the timer expires. Do NOT re-add your account in Developer Options during this wait.",
    "20038": "Device locked by Find Device — Sign in at i.mi.com/mobile/find and disable Locate Device.",
    "20041": "No phone number linked — Link a phone number at account.xiaomi.com/pass/serviceLogin?checkSafePhone=true.",
    "20045": "Region mismatch — Account registration region does not match device sale region (e.g., Global account on CN device). Change dataCenterZone.",
    "30002": "Mi Community authorization required (HyperOS) — Apply for permission using Option [05] (miapply) or the Xiaomi Community app, then bind in Mi Unlock Status.",
}


def _find_binary(name: str) -> str:
    found = shutil.which(name)
    if found:
        return found
    home_pt = Path.home() / "platform-tools" / (f"{name}.exe" if os.name == "nt" else name)
    if home_pt.exists():
        return str(home_pt)
    return name


class XboyrexMiToolGUI:
    """Modern Desktop GUI for MiTool + MiUnlockTool by xboyrex."""

    def __init__(self):
        install_smart_dns_hook()
        deps.patch_subtool_links()

        if _HAS_CTK:
            ctk.set_appearance_mode("Dark")
            self.root = ctk.CTk()
            self.root.configure(fg_color=BG_DARK)
        else:
            self.root = tk.Tk()
            self.root.configure(bg=BG_DARK)

        self.root.title(f"MiTool + MiUnlockTool by xboyrex v{__version__} — Official Desktop Suite")
        self.root.geometry("1080x700")
        self.root.minsize(960, 620)

        # Load custom XBOYREX window icon
        try:
            here = Path(__file__).resolve().parent
            ico_p = here / "app_icon.ico"
            png_p = here / "app_icon.png"
            if os.name == "nt" and ico_p.exists():
                self.root.iconbitmap(str(ico_p))
            elif png_p.exists():
                self._icon_img = tk.PhotoImage(file=str(png_p))
                self.root.iconphoto(True, self._icon_img)
        except Exception:
            pass

        self.active_proc: subprocess.Popen | None = None
        self.log_queue: queue.Queue[str] = queue.Queue()

        self._build_ui()
        self.root.after(80, self._poll_log_queue)
        self.log_message("╔════════════════════════════ [ XBOYREX EDITION ] ═════════════════════════════╗")
        self.log_message(f"║  MITOOL + MIUNLOCKTOOL UNIFIED DESKTOP SUITE  v{__version__:<29} ║")
        self.log_message("║  Developed by xboyrex (Mayank Droid) · Smart DoH DNS (1.1.1.1 / 8.8.8.8) ON  ║")
        self.log_message("╚══════════════════════════════════════════════════════════════════════════════╝\n")
        self.log_message("▸ Select any operation on the left panel ([01]–[05]) or inspect a connected Fastboot/ADB device.\n")

    def _build_ui(self):
        # Top Header Bar
        top_frame = tk.Frame(self.root, bg=CARD_BG, highlightbackground=CRIMSON, highlightthickness=2)
        top_frame.pack(fill="x", padx=14, pady=(12, 8))

        title_col = tk.Frame(top_frame, bg=CARD_BG)
        title_col.pack(side="left", padx=16, pady=10)

        tk.Label(
            title_col,
            text="X B O Y R E X   •   M I T O O L   &   M I U N L O C K   S T U D I O",
            font=("Segoe UI", 16, "bold"),
            fg=TEXT_WHITE,
            bg=CARD_BG,
        ).pack(anchor="w")

        tk.Label(
            title_col,
            text=f"v{__version__} Unified Edition  •  Cross-Platform • Python • Fastboot • ADB • Smart DoH DNS",
            font=("Consolas", 10),
            fg=CRIMSON,
            bg=CARD_BG,
        ).pack(anchor="w", pady=(2, 0))

        # Social & Quick Links Row (Right side of top bar)
        links_col = tk.Frame(top_frame, bg=CARD_BG)
        links_col.pack(side="right", padx=16, pady=10)

        social_buttons = [
            ("Telegram", "#26A5E4", TELEGRAM_URL),
            ("YouTube", CRIMSON, YOUTUBE_URL),
            ("GitHub", "#24292F", GITHUB_URL),
            ("Website", CARD_ELEVATED, WEBSITE_URL),
        ]
        for label, color_hex, url in social_buttons:
            btn = tk.Button(
                links_col,
                text=label,
                font=("Segoe UI", 9, "bold"),
                fg=TEXT_WHITE,
                bg=color_hex,
                activebackground=CRIMSON_HOVER,
                activeforeground=TEXT_WHITE,
                relief="flat",
                padx=12,
                pady=5,
                cursor="hand2",
                command=lambda u=url: webbrowser.open(u, new=2),
            )
            btn.pack(side="left", padx=4)

        # Main Split Container (Left: Operations & Device Controls | Right: Interactive Terminal Console)
        body = tk.Frame(self.root, bg=BG_DARK)
        body.pack(fill="both", expand=True, padx=14, pady=(0, 12))

        left_panel = tk.Frame(body, bg=CARD_BG, width=410, highlightbackground=BORDER_COLOR, highlightthickness=1)
        left_panel.pack(side="left", fill="y", padx=(0, 10))
        left_panel.pack_propagate(False)

        right_panel = tk.Frame(body, bg=CARD_BG, highlightbackground=BORDER_COLOR, highlightthickness=1)
        right_panel.pack(side="left", fill="both", expand=True)

        # --- Left Panel: Section 1 — Execution Pipeline [01]-[05] ---
        tk.Label(
            left_panel,
            text="[ EXECUTION PIPELINE • 01–05 ]",
            font=("Consolas", 11, "bold"),
            fg=CRIMSON,
            bg=CARD_BG,
        ).pack(anchor="w", padx=14, pady=(12, 6))

        for code_num, title, cmd, tooltip in OPERATIONS:
            card = tk.Frame(left_panel, bg=CARD_ELEVATED, highlightbackground=BORDER_COLOR, highlightthickness=1)
            card.pack(fill="x", padx=12, pady=4)

            row = tk.Frame(card, bg=CARD_ELEVATED)
            row.pack(fill="x", padx=10, pady=6)

            tk.Label(
                row,
                text=f"[{code_num}]",
                font=("Consolas", 11, "bold"),
                fg=CRIMSON,
                bg=CARD_ELEVATED,
            ).pack(side="left", padx=(0, 8))

            txt_col = tk.Frame(row, bg=CARD_ELEVATED)
            txt_col.pack(side="left", fill="x", expand=True)

            tk.Label(
                txt_col,
                text=title,
                font=("Segoe UI", 10, "bold"),
                fg=TEXT_WHITE,
                bg=CARD_ELEVATED,
                anchor="w",
            ).pack(fill="x")

            tk.Label(
                txt_col,
                text=tooltip,
                font=("Segoe UI", 8),
                fg=TEXT_MUTED,
                bg=CARD_ELEVATED,
                anchor="w",
                wraplength=240,
                justify="left",
            ).pack(fill="x")

            run_btn = tk.Button(
                row,
                text="LAUNCH",
                font=("Consolas", 9, "bold"),
                fg=TEXT_WHITE,
                bg=CRIMSON,
                activebackground=CRIMSON_HOVER,
                activeforeground=TEXT_WHITE,
                relief="flat",
                padx=10,
                pady=4,
                cursor="hand2",
                command=lambda c=cmd, t=title: self.start_subtool_process(c, t),
            )
            run_btn.pack(side="right", padx=(6, 0))

        # --- Left Panel: Section 2 — Fastboot / ADB Quick Actions & Error Lookup ---
        tk.Label(
            left_panel,
            text="[ FASTBOOT / ADB & DIAGNOSTICS ]",
            font=("Consolas", 11, "bold"),
            fg=CRIMSON,
            bg=CARD_BG,
        ).pack(anchor="w", padx=14, pady=(14, 6))

        util_grid = tk.Frame(left_panel, bg=CARD_BG)
        util_grid.pack(fill="x", padx=12)

        btn_specs = [
            ("Inspect Device (ADB/FB)", self.inspect_device_action),
            ("Read Fastboot Token", self.read_fastboot_token_action),
            ("Ping Xiaomi CDNs & DoH", self.ping_cdns_action),
            ("Stop Running Task", self.stop_active_process),
        ]
        for idx, (b_label, b_cmd) in enumerate(btn_specs):
            r_i, c_i = divmod(idx, 2)
            b = tk.Button(
                util_grid,
                text=b_label,
                font=("Segoe UI", 9, "bold"),
                fg=TEXT_WHITE,
                bg="#1E2638" if idx < 3 else "#7F1D1D",
                activebackground=CRIMSON,
                activeforeground=TEXT_WHITE,
                relief="flat",
                pady=6,
                cursor="hand2",
                command=b_cmd,
            )
            b.grid(row=r_i, column=c_i, sticky="ew", padx=3, pady=3)
        util_grid.columnconfigure(0, weight=1)
        util_grid.columnconfigure(1, weight=1)

        # Error Code Lookup Bar
        err_frame = tk.Frame(left_panel, bg=CARD_BG)
        err_frame.pack(fill="x", padx=12, pady=(10, 4))

        tk.Label(
            err_frame,
            text="Error Code Lookup:",
            font=("Segoe UI", 9, "bold"),
            fg=TEXT_MUTED,
            bg=CARD_BG,
        ).pack(side="left")

        self.err_code_var = tk.StringVar(value="20036")
        err_menu = tk.OptionMenu(err_frame, self.err_code_var, *ERROR_CODE_GUIDE.keys())
        err_menu.configure(bg=CARD_ELEVATED, fg=TEXT_WHITE, highlightthickness=0, relief="flat", font=("Consolas", 9))
        err_menu.pack(side="left", padx=6)

        tk.Button(
            err_frame,
            text="Explain Code",
            font=("Segoe UI", 9, "bold"),
            fg=TEXT_WHITE,
            bg=CRIMSON,
            activebackground=CRIMSON_HOVER,
            activeforeground=TEXT_WHITE,
            relief="flat",
            padx=8,
            command=self.explain_selected_code,
        ).pack(side="left")

        # Footer Credit
        tk.Label(
            left_panel,
            text="Developed by xboyrex (Mayank Droid) · Apache-2.0",
            font=("Consolas", 8),
            fg=TEXT_MUTED,
            bg=CARD_BG,
        ).pack(side="bottom", pady=8)

        # --- Right Panel: Live Execution Console + Interactive Input Prompt ---
        con_header = tk.Frame(right_panel, bg=CARD_BG)
        con_header.pack(fill="x", padx=14, pady=(10, 6))

        tk.Label(
            con_header,
            text="LIVE EXECUTION CONSOLE",
            font=("Consolas", 11, "bold"),
            fg=TEXT_WHITE,
            bg=CARD_BG,
        ).pack(side="left")

        tk.Button(
            con_header,
            text="Clear Console",
            font=("Segoe UI", 9, "bold"),
            fg=TEXT_WHITE,
            bg=CARD_ELEVATED,
            activebackground=CRIMSON,
            activeforeground=TEXT_WHITE,
            relief="flat",
            padx=10,
            command=self.clear_console,
        ).pack(side="right")

        self.console_text = tk.Text(
            right_panel,
            bg="#05070B",
            fg=TEXT_WHITE,
            insertbackground=CRIMSON,
            font=("Consolas", 10),
            relief="flat",
            wrap="word",
            padx=12,
            pady=10,
        )
        self.console_text.pack(fill="both", expand=True, padx=12, pady=(0, 8))

        # Interactive Stdin Bar (allows sending choices like `1`, `2`, `m`, `Enter`, or URLs to running tools!)
        input_bar = tk.Frame(right_panel, bg=CARD_BG)
        input_bar.pack(fill="x", padx=12, pady=(0, 12))

        tk.Label(
            input_bar,
            text="Interactive Input ›",
            font=("Consolas", 10, "bold"),
            fg=CRIMSON,
            bg=CARD_BG,
        ).pack(side="left", padx=(0, 8))

        self.stdin_entry = tk.Entry(
            input_bar,
            bg="#05070B",
            fg=TEXT_WHITE,
            insertbackground=CRIMSON,
            font=("Consolas", 10),
            relief="flat",
            highlightbackground=BORDER_COLOR,
            highlightthickness=1,
        )
        self.stdin_entry.pack(side="left", fill="x", expand=True, ipady=5)
        self.stdin_entry.bind("<Return>", lambda _e: self.send_stdin_to_process())

        tk.Button(
            input_bar,
            text="Send / Enter",
            font=("Consolas", 9, "bold"),
            fg=TEXT_WHITE,
            bg=CRIMSON,
            activebackground=CRIMSON_HOVER,
            activeforeground=TEXT_WHITE,
            relief="flat",
            padx=14,
            pady=5,
            cursor="hand2",
            command=self.send_stdin_to_process,
        ).pack(side="left", padx=(8, 0))

    def log_message(self, msg: str):
        self.log_queue.put(msg)

    def _poll_log_queue(self):
        while not self.log_queue.empty():
            msg = self.log_queue.get_nowait()
            # Strip raw ANSI escape codes for clean GUI text rendering
            import re
            clean = re.sub(r"\x1b\[[0-9;]*[a-zA-Z]", "", msg)
            clean = re.sub(r"\x1b\]8;;.*?\x1b\\", "", clean)
            self.console_text.insert("end", clean + "\n")
            self.console_text.see("end")
        self.root.after(80, self._poll_log_queue)

    def clear_console(self):
        self.console_text.delete("1.0", "end")

    def explain_selected_code(self):
        code = self.err_code_var.get()
        explanation = ERROR_CODE_GUIDE.get(code, "Unknown error code.")
        self.log_message(f"\n[DIAGNOSTIC • CODE {code}]\n  ▸ {explanation}\n")

    def inspect_device_action(self):
        def worker():
            self.log_message("\n[▸] Scanning connected Xiaomi devices via Fastboot & ADB...")
            fb = _find_binary("fastboot")
            adb = _find_binary("adb")
            found_any = False

            try:
                p_fb = subprocess.run([fb, "devices"], capture_output=True, text=True, timeout=5)
                if p_fb.stdout.strip():
                    found_any = True
                    self.log_message(f"  ✔ Fastboot Device: {p_fb.stdout.strip()}")
                    for var in ("product", "unlocked", "anti", "current-slot"):
                        pv = subprocess.run([fb, "getvar", var], capture_output=True, text=True, timeout=4)
                        combined = f"{pv.stdout}\n{pv.stderr}"
                        for ln in combined.splitlines():
                            if f"{var}:" in ln:
                                self.log_message(f"    • {var}: {ln.split(':', 1)[1].strip()}")
                                break
            except Exception:
                pass

            try:
                p_adb = subprocess.run([adb, "devices"], capture_output=True, text=True, timeout=5)
                lines = [l for l in p_adb.stdout.splitlines()[1:] if l.strip() and not l.startswith("*")]
                if lines:
                    found_any = True
                    self.log_message(f"  ✔ ADB Device: {lines[0]}")
            except Exception:
                pass

            if not found_any:
                self.log_message("  ! No Xiaomi device detected in Fastboot or ADB mode. Check USB cable & drivers.")

        threading.Thread(target=worker, daemon=True).start()

    def read_fastboot_token_action(self):
        def worker():
            self.log_message("\n[▸] Querying Fastboot product & deviceToken...")
            fb = _find_binary("fastboot")
            try:
                for args in (["getvar", "product"], ["oem", "get_token"], ["getvar", "token"]):
                    proc = subprocess.run([fb] + args, capture_output=True, text=True, timeout=6)
                    out = (proc.stdout + "\n" + proc.stderr).strip()
                    if out:
                        self.log_message(f"  [{ ' '.join(args) }]\n{out}")
            except Exception as e:
                self.log_message(f"  ✗ Fastboot query error: {e}")

        threading.Thread(target=worker, daemon=True).start()

    def ping_cdns_action(self):
        def worker():
            import socket
            self.log_message("\n[▸] Probing Xiaomi Regional Unlock & Auth Gateways (Smart DoH DNS Active)...")
            hosts = [
                ("India Unlock CDN", "in-unlock.update.intl.miui.com"),
                ("Global (SG) CDN", "unlock.update.intl.miui.com"),
                ("Europe Unlock CDN", "eu-unlock.update.intl.miui.com"),
                ("Mi Community API", "sgp-api.buy.mi.com"),
                ("Xiaomi Account Auth", "account.xiaomi.com"),
            ]
            for label, h in hosts:
                ip, resolver = resolve_host_smart(h)
                if not ip:
                    self.log_message(f"  ✗ {label:<22} ({h}) -> Unresolved")
                    continue
                t0 = time.perf_counter()
                try:
                    with socket.create_connection((ip, 443), timeout=2.0):
                        ms = (time.perf_counter() - t0) * 1000.0
                        self.log_message(f"  ✔ {label:<22} -> {ip:<15} | {ms:5.1f} ms ({resolver})")
                except Exception:
                    self.log_message(f"  ! {label:<22} -> {ip:<15} | TIMEOUT")

        threading.Thread(target=worker, daemon=True).start()

    def stop_active_process(self):
        if self.active_proc and self.active_proc.poll() is None:
            try:
                self.active_proc.terminate()
                self.log_message("\n[!] Terminated running operation.")
            except Exception:
                pass
        else:
            self.log_message("\n[•] No active operation is currently running.")

    def send_stdin_to_process(self):
        val = self.stdin_entry.get()
        self.stdin_entry.delete(0, "end")
        if self.active_proc and self.active_proc.poll() is None and self.active_proc.stdin:
            try:
                self.log_message(f"› {val}")
                self.active_proc.stdin.write(val + "\n")
                self.active_proc.stdin.flush()
            except Exception as e:
                self.log_message(f"[✗] Failed to send input: {e}")
        else:
            self.log_message(f"› {val} (No active process waiting for input)")

    def start_subtool_process(self, cmd: str, title: str):
        if self.active_proc and self.active_proc.poll() is None:
            messagebox.showwarning("Operation Running", "An operation is already running. Click 'Stop Running Task' first.")
            return

        def worker():
            self.log_message(f"\n┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓")
            self.log_message(f"┃  LAUNCHING: {title:<64} ┃")
            self.log_message(f"┃  How It Works: {GITHUB_URL:<61} ┃")
            self.log_message(f"┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛")

            runner_code = (
                f"import sys; from mitool.cli import launch_subtool; "
                f"sys.exit(launch_subtool({cmd!r}))"
            )
            env = os.environ.copy()
            env["PYTHONUNBUFFERED"] = "1"
            try:
                self.active_proc = subprocess.Popen(
                    [sys.executable, "-u", "-c", runner_code],
                    stdin=subprocess.PIPE,
                    stdout=subprocess.PIPE,
                    stderr=subprocess.STDOUT,
                    text=True,
                    bufsize=1,
                    env=env,
                )
                for line in iter(self.active_proc.stdout.readline, ""):
                    if not line:
                        break
                    self.log_message(line.rstrip("\r\n"))
                rc = self.active_proc.wait()
                self.log_message(f"\n[✔] Operation '{cmd}' finished (exit code {rc}).")
            except Exception as e:
                self.log_message(f"\n[✗] Failed to launch '{cmd}': {e}")

        threading.Thread(target=worker, daemon=True).start()

    def run(self):
        self.root.mainloop()


def main():
    if not _HAS_TK:
        from mitool.cli import console, CRIMSON, WHITE, main as cli_main
        console.print(f"\n[{CRIMSON}]![/] [{WHITE}]Graphical display (Tkinter) is not installed in this headless/Termux shell. Launching Terminal CLI...[/]\n")
        return cli_main()
    app = XboyrexMiToolGUI()
    app.run()


if __name__ == "__main__":
    main()
