# Mi Unlock Tool 8.5.0 — Source Audit & Fix Report

## Scope

- Primary source audited: `MiUnlockTool-main.zip` (Android app, package `com.xboyrex.miunlocktool`, version 8.5.0).
- `YTXTube-main.zip` is a separate application and was not modified.
- `miunlocktool-main (1).zip` is a different/older Xiaomi Unlock Tool Revamp codebase (v8.2, package `com.asinsideout.miunlocktool`, desktop Python + Android preview). It was not merged into this app because mixing the two projects would replace unrelated architecture and package IDs.

## Confirmed findings fixed

The audit found **18 concrete source/configuration findings** (including four separate vector-resource occurrences). The counts below distinguish repeated resource occurrences from root causes.

### Android resources and build

1. Four vector drawable files referenced `?attr/colorControlNormal` without a resolvable app attribute. Replaced with the Android framework attribute.
2. One adaptive launcher icon was placed in `mipmap-anydpi` instead of the API-26-qualified `mipmap-anydpi-v26` directory. Removed the invalid duplicate; kept both valid v26 launcher definitions.
3. Replaced wrapper-dependent CI build with a provisioned Gradle 9.8.2 installation, explicit JDK 21, Android SDK API 37 setup, unit-test execution, APK verification, artifact upload, and automatic release publication.
4. Updated the tagged-release workflow to use the same toolchain path.

### UI / responsive layout

5. Clock header action chips and the token-card Account Login action were laid out in fixed horizontal rows. On narrow screens the text collapsed vertically, matching the supplied screenshot. Added compact-width stacked layouts and single-line action labels.

### Runtime correctness

6. Ping measurement returned a fabricated 165 ms value when no HTTP/TCP samples were available. Unavailable values now remain `-1`; the combined average uses only real successful samples.
7. `httpPing(timeoutMs)` ignored its timeout argument. The per-call OkHttp client now applies connect, read, and call timeouts.
8. `measureAveragePingMs(count)` ignored the requested sample count. It now samples the requested bounded count and averages valid samples.
9. Manual mode exposed Burst Count and Burst Interval controls but sent only one request. It now uses the configured count/interval and stops on success, token expiry, blocked/too-new status, or transport failure.
10. Automatic NTP and ping settings were saved by the UI but ignored by scheduler triggers. The scheduler now respects both preferences.
11. Latency compensation was calculated but not applied to actual scheduled request targets. Measured latency is now subtracted from configured target times; when no valid measurement exists, raw configured timings are used.
12. Free-form response strings containing words such as `success`, `ok`, or `approved` could be interpreted as successful API responses. Removed keyword-based success inference; structured numeric codes/results are required.
13. Account status could be marked approved from a free-form success/approved string. Removed that fallback.
14. SNTP responses were accepted without validating packet length, leap indicator, NTP version, server mode, stratum, or echoed originate timestamp. Added these checks.
15. Fastboot token hex parsing accepted odd-length/non-hex input and could throw an indexing error. Added strict validation and converted invalid input into a failed `Result`.
16. Device IDs used `java.util.Random`; switched to `SecureRandom`.

### Security and privacy hardening

17. Cleartext network traffic was enabled despite the app's API endpoints using HTTPS. Disabled cleartext traffic.
18. Android backup was enabled while account/session values are persisted locally. Disabled app backup/data extraction.

## Regression tests added

- Xiaomi API parser: 4 tests covering false-positive free-form success, structured success, and explicit quota code.
- Ping aggregation: 3 tests covering no samples, one available transport, and both available transports.
- Fastboot hex parser: 4 tests covering valid, odd-length, non-hex, and empty input.

Total: **11 regression tests added**.

## Verification performed

- `tools/verify_source.py`: **71 static integrity checks passed**.
- Android resource XML: all 24 XML files parsed successfully.
- GitHub Actions YAML: both workflow files parsed successfully with PyYAML.
- ZIP input integrity: original archives passed ZIP CRC validation.
- Signing identity: retained `app/release.keystore` because it is the existing shared Android debug key and removing it would change the APK signing fingerprint, potentially preventing in-place upgrades.

## Important limitations / unresolved risks

1. **The bundled keystore is a shared Android debug keystore**, alias `androiddebugkey`, using the default debug password. It is retained only to preserve the existing APK signature and upgrade compatibility. It is not a secure production signing identity. Anyone with the public source can reproduce its signature. A private signing key stored in GitHub Actions secrets is required for secure production releases.
2. Xiaomi credentials are still stored in app-private SharedPreferences. Backup is disabled, but values are not encrypted with Android Keystore. Rooted-device/local compromise remains a risk.
3. No live Xiaomi account, real token, supported handset, USB fastboot device, or Xiaomi backend was available for end-to-end verification. API availability, account eligibility, quota acceptance, and bootloader unlock success cannot be guaranteed by static source analysis.
4. This environment did not have Android SDK/Gradle installed, so a full Android compile and device runtime test could not be executed here. The updated GitHub Actions workflow runs the added unit tests and `:app:assembleRelease`; the resulting run is the authoritative build verification.
5. The app's bootloader-unlock path is destructive by design on supported devices and can wipe user data. The user must confirm device/account eligibility and back up data before any actual unlock operation.

## Build/release behavior

On a push to `main` or `master`, GitHub Actions will:

1. Set up JDK 21 and Android SDK API 37.
2. Install Gradle 9.8.2 independently of the Gradle wrapper download endpoint.
3. Run the static source audit and 11 unit tests.
4. Build `:app:assembleRelease`.
5. Verify and upload generated APKs.
6. Publish a GitHub Release only after the build job succeeds.

A successful static audit is not the same as a successful Android build. The Actions run must finish green before an APK/release can be considered verified.
