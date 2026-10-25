#!/data/data/com.termux/files/usr/bin/bash
# ==============================================================================
# MiTool + MiUnlockTool Unified Suite v3.0 by xboyrex — Self-Healing Installer
# One-Liner:
#   curl -sS https://raw.githubusercontent.com/xboyrex/MiTool/main/install.sh | bash
# ==============================================================================

set -e

R="\033[1;31m"
G="\033[1;32m"
W="\033[1;37m"
D="\033[0;90m"
N="\033[0m"

TOTAL_STEPS=7
STEP=0

# Auto-heal broken Termux mirror (e.g. mirrors.hust.edu.cn hash mismatch)
fix_termux_mirror() {
    mkdir -p "$PREFIX/etc/apt/sources.list.d"
    echo "deb https://packages-cf.termux.dev/apt/termux-main stable main" > "$PREFIX/etc/apt/sources.list"
    rm -rf "$PREFIX/var/lib/apt/lists/"* 2>/dev/null || true
    apt-get clean >/dev/null 2>&1 || true
}

run_step() {
    STEP=$((STEP + 1))
    local msg="$1" cmd="$2" output

    printf "${R}[%d/%d]${N} %s... " "$STEP" "$TOTAL_STEPS" "$msg"

    if output=$(bash -c "$cmd" 2>&1); then
        echo -e "${G}✔${N}"
    else
        # Self-heal mirror & retry once if apt/pkg failed due to mirror sync mismatch
        fix_termux_mirror
        if output=$(bash -c "$cmd" 2>&1); then
            echo -e "${G}✔ (Mirror Auto-Healed)${N}"
        else
            echo -e "${R}✘${N}"
            echo -e "${R}Error occurred during: $msg${N}"
            echo "$output"
            exit 1
        fi
    fi
}

echo
echo -e "  ${R}X B O Y R E X${N}   ${D}•${N}   ${W}MI TOOL UNIFIED v3.0 INSTALLER${N}"
echo

arch=$(dpkg --print-architecture 2>/dev/null || uname -m)
if [[ "$arch" != "aarch64" && "$arch" != "arm" && "$arch" != "armv7l" && "$arch" != "armv8l" ]]; then
    echo "MiTool Termux installer does not support architecture $arch"
    exit 1
fi

if [ ! -d "$HOME/storage" ]; then
    echo -e "\nGrant permission: termux-setup-storage\nThen rerun the command.\n"
    exit 1
fi

if ! cmd package list packages --user 0 com.termux.api < /dev/null 2>/dev/null | grep -q "com.termux.api"; then
    echo -e "\ncom.termux.api app is not installed\nPlease install it first\n"
    exit 1
fi

# Switch away from broken mirrors.hust.edu.cn automatically if present
if grep -q "hust.edu.cn" "$PREFIX/etc/apt/sources.list" 2>/dev/null; then
    fix_termux_mirror
fi

run_step "Updating system & fixing broken packages" \
    "DEBIAN_FRONTEND=noninteractive apt-get --fix-broken install -y && apt-get update -y && DEBIAN_FRONTEND=noninteractive apt-get upgrade -y -o Dpkg::Options::=--force-confnew"

run_step "Installing Python3 & Git" \
    "command -v python3 >/dev/null 2>&1 && command -v git >/dev/null 2>&1 || DEBIAN_FRONTEND=noninteractive apt-get install -y python git"

run_step "Installing python-pip" \
    "command -v pip >/dev/null 2>&1 || DEBIAN_FRONTEND=noninteractive apt-get install -y python-pip"

run_step "Installing libusb" \
    "test -f \"\$PREFIX/lib/libusb-1.0.so\" || DEBIAN_FRONTEND=noninteractive apt-get install -y libusb"

run_step "Installing termux-api" \
    "command -v termux-usb >/dev/null 2>&1 || DEBIAN_FRONTEND=noninteractive apt-get install -y termux-api"

run_step "Installing termux-adb & fastboot" \
    "curl -fsSL https://raw.githubusercontent.com/nohajc/termux-adb/master/install.sh | bash && ln -sf \$PREFIX/bin/termux-fastboot \$PREFIX/bin/fastboot && ln -sf \$PREFIX/bin/termux-adb \$PREFIX/bin/adb"

run_step "Installing MiTool + MiUnlockTool Unified Suite from GitHub" \
    "pip install -U --force-reinstall --no-deps miapply && pip install -U pymitool miunlock miflash miasst fcetool migate pyaes rich requests && pip install -U --no-deps --force-reinstall git+https://github.com/xboyrex/MiTool.git && python3 -c \"import mitool.deps as d; d.patch_subtool_links()\""

echo -e "\n${G}✔ Installation completed successfully!${N}"
echo -e "\nRun command: ${G}mitool${N} (or ${R}miunlock${N})\n"
