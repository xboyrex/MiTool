#!/usr/bin/env python3
"""Offline integrity checks for the Mi Unlock Tool Android source tree."""
from __future__ import annotations

import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
errors: list[str] = []
checks = 0

def require(condition: bool, message: str) -> None:
    global checks
    checks += 1
    if not condition:
        errors.append(message)

# Android resource XML must be well-formed.
resource_xml = list((ROOT / "app/src/main/res").rglob("*.xml"))
for xml_file in resource_xml:
    try:
        ET.parse(xml_file)
    except ET.ParseError as exc:
        errors.append(f"Malformed resource XML: {xml_file.relative_to(ROOT)}: {exc}")
checks += len(resource_xml)

res = ROOT / "app/src/main/res"
require(not (res / "mipmap-anydpi/ic_launcher.xml").exists(),
        "Adaptive launcher icon must not be in mipmap-anydpi (requires v26 qualifier).")
require((res / "mipmap-anydpi-v26/ic_launcher.xml").is_file(),
        "Missing mipmap-anydpi-v26/ic_launcher.xml.")
require((res / "mipmap-anydpi-v26/ic_launcher_round.xml").is_file(),
        "Missing mipmap-anydpi-v26/ic_launcher_round.xml.")

drawable_xml = list((res / "drawable").glob("*.xml"))
for xml_file in drawable_xml:
    content = xml_file.read_text(encoding="utf-8")
    require("?attr/colorControlNormal" not in content,
            f"Unresolved app theme attribute remains in {xml_file.relative_to(ROOT)}.")

ping_file = ROOT / "app/src/main/java/com/xboyrex/miunlocktool/data/PingTester.kt"
ping = ping_file.read_text(encoding="utf-8")
require("else 165L" not in ping and "else -> 165L" not in ping,
        "Synthetic 165ms ping fallback remains.")
require("aggregatePingResults" in ping and "?: -1L" in ping,
        "Ping aggregation must represent unavailable samples as -1ms.")
require(".callTimeout(timeoutMs.toLong()" in ping,
        "httpPing timeout argument is not applied to the HTTP call.")

scheduler_file = ROOT / "app/src/main/java/com/xboyrex/miunlocktool/service/ApplyScheduler.kt"
scheduler = scheduler_file.read_text(encoding="utf-8")
require("prefs.autoNtpEnabled" in scheduler,
        "Automatic NTP preference is not consumed by the scheduler.")
require("prefs.autoPingEnabled" in scheduler and
        "if (prefs.autoPingEnabled) {\n            measurePing(autoTriggered = true)" in scheduler,
        "Automatic ping preference is not consumed consistently by scheduler startup.")
view_model = (ROOT / "app/src/main/java/com/xboyrex/miunlocktool/ui/MainViewModel.kt").read_text(encoding="utf-8")
require("if (prefs.autoNtpEnabled) syncNtp()" in view_model and
        "        measurePing()\n" not in view_model,
        "ViewModel startup bypasses automatic sync preferences.")
require("latencyOffsetSeconds" in scheduler and
        "map { (it - latencyOffsetSeconds)" in scheduler,
        "Measured latency is not applied to scheduled timing targets.")
require("prefs.burstCount" in scheduler and "prefs.burstIntervalMs" in scheduler,
        "Manual burst settings are saved but not consumed by the scheduler.")

api_file = ROOT / "app/src/main/java/com/xboyrex/miunlocktool/data/XiaomiApiService.kt"
api = api_file.read_text(encoding="utf-8")
require('lower.contains("success")' not in api and
        'lower.contains("approved")' not in api,
        "Free-form response text can still be interpreted as success.")
require("applyResult = matchApplyResult" in api,
        "Parser still fabricates apply_result from a generic success code.")

sntp_file = ROOT / "app/src/main/java/com/xboyrex/miunlocktool/data/SntpClient.kt"
sntp = sntp_file.read_text(encoding="utf-8")
for token in ["leapIndicator == 3", "mode != 4", "stratum !in 1..15",
              "clientTransmitTimestamp", "responsePacket.length < NTP_PACKET_SIZE"]:
    require(token in sntp, f"SNTP validation missing: {token}")

manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
require('android:usesCleartextTraffic="false"' in manifest,
        "Cleartext network traffic must be disabled.")
require('android:allowBackup="false"' in manifest,
        "Application backup must be disabled while credentials are stored locally.")

require((ROOT / "app/release.keystore").is_file(),
        "Existing shared signing key is missing; APK upgrade signature would change.")
gitignore = (ROOT / ".gitignore").read_text(encoding="utf-8")
require("!app/release.keystore" in gitignore,
        "Existing signing key is no longer included; in-place APK upgrades may fail.")

screens = (ROOT / "app/src/main/java/com/xboyrex/miunlocktool/Screens.kt").read_text(encoding="utf-8")
require("BoxWithConstraints" in screens and "compactHeader" in screens,
        "Responsive compact-width header layout is missing.")

workflow = (ROOT / ".github/workflows/build.yml").read_text(encoding="utf-8")
for token in ["gradle-version: '9.8.2'", "platforms;android-37.0",
              ":app:testDebugUnitTest", ":app:assembleRelease",
              "softprops/action-gh-release@v2", "contents: write"]:
    require(token in workflow, f"CI workflow missing required configuration: {token}")

require((ROOT / "app/src/test/java/com/xboyrex/miunlocktool/data/XiaomiApiServiceTest.kt").is_file(),
        "Missing API response regression tests.")
require((ROOT / "app/src/test/java/com/xboyrex/miunlocktool/data/PingTesterTest.kt").is_file(),
        "Missing ping aggregation regression tests.")
require((ROOT / "app/src/test/java/com/xboyrex/miunlocktool/unlock/MiUnlockCryptoTest.kt").is_file(),
        "Missing token hex validation regression tests.")
crypto = (ROOT / "app/src/main/java/com/xboyrex/miunlocktool/unlock/MiUnlockCrypto.kt").read_text(encoding="utf-8")
require("even number of characters" in crypto and "non-hexadecimal character" in crypto,
        "Fastboot token hex input is not strictly validated.")
workflow_source = (ROOT / ".github/workflows/release.yml").read_text(encoding="utf-8")
require("gradle-version: '9.8.2'" in workflow_source and
        ":app:assembleRelease" in workflow_source,
        "Tagged release workflow still relies on the failing Gradle wrapper.")

print(f"MiUnlockTool source audit: {checks} checks executed.")
if errors:
    print(f"FAILED: {len(errors)} issue(s)")
    for error in errors:
        print(f" - {error}")
    sys.exit(1)

print("PASS: all configured static integrity checks.")
