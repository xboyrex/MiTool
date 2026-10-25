import sys
from miunlock.system import (
    console,
    GREEN,
    WHITE,
    ORANGE,
    CRIMSON,
    DIM,
    VERSION,
    install_smart_dns_and_color_hooks,
    render_xboyrex_startup,
)
from miunlock.unlock import unlock_device
from miunlock.fastboot import get_fastboot


def main():
    if "--version" in sys.argv or "-v" in sys.argv:
        console.print(f"MiUnlockTool by xboyrex v{VERSION}")
        return 0

    install_smart_dns_and_color_hooks()
    render_xboyrex_startup()

    console.print(f"\n[{CRIMSON}][01][/{CRIMSON}] [{WHITE}]Environment Check[/{WHITE}] [{DIM}]— Locating fastboot binary[/]")
    fastboot_cmd = get_fastboot()
    console.print(f"  [{GREEN}]✔[/] [{DIM}]Fastboot Binary:[/] [{GREEN}]{fastboot_cmd}[/]")

    console.print(f"\n[{CRIMSON}][02][/{CRIMSON}] [{WHITE}]Xiaomi Account Login[/{WHITE}] [{DIM}]— Authenticating session via Xiaomi Account[/]\n")
    from migate import get_passtoken, get_service, get_region, get_dataCenterZone

    param = {"sid": "unlockApi"}
    param["checkSafeAddress"] = True

    passToken = get_passtoken(param)
    if passToken is None:
        raise SystemExit(1)

    service = get_service(passToken, param)
    if service is None:
        raise SystemExit(1)

    region = get_region(passToken)
    if region is None:
        userId = passToken["userId"]
        Zone = get_dataCenterZone(userId)
    else:
        console.print(f"\n[{GREEN}]Account Region: {region}[/]")
        if region == "CN":
            Zone = "China"
        elif region == "IN":
            Zone = "India"
        elif region == "RU":
            Zone = "Russia"
        elif region in {
            "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR", "DE",
            "EL", "HU", "IS", "IE", "IT", "LV", "LI", "LT", "LU", "MT", "NL",
            "NO", "PL", "PT", "RO", "SK", "SI", "ES", "SE", "UK",
        }:
            Zone = "Europe"
        else:
            Zone = "Singapore"

    if Zone is None:
        Zone = get_dataCenterZone()
    else:
        console.print(f"\n[{GREEN}]dataCenterZone: {Zone}[/]")
        user_input = console.input(
            f"\n[{WHITE}](Enter to continue, [{ORANGE}]m[/] to change dataCenterZone) > [/]"
        ).strip().lower()
        if user_input == "m":
            Zone = get_dataCenterZone()

    domain = {
        "Singapore": "https://unlock.update.intl.miui.com",
        "China": "https://unlock.update.miui.com",
        "India": "https://in-unlock.update.intl.miui.com",
        "Russia": "https://ru-unlock.update.intl.miui.com",
        "Europe": "https://eu-unlock.update.intl.miui.com",
    }.get(Zone)

    unlock_device(domain, service, fastboot_cmd)
