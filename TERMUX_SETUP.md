# Termux deployment

1. Download `MiUnlockTool-Fixed-8.5.0.zip` to Android `Download`.
2. Download `termux-push-miunlocktool.sh` to Android `Download`.
3. In Termux, run:

```bash
bash ~/storage/shared/Download/termux-push-miunlocktool.sh
```

The script validates the ZIP, runs `tools/verify_source.py`, synchronizes the project to `xboyrex/MiTool`, pushes using the existing Git credential helper / GitHub CLI login, then polls GitHub Actions for the Android build and release result.

No PAT is requested or embedded. If the existing Git credential helper does not have write access, GitHub will reject the push.

The build runs on GitHub Actions; Termux does not need a local Android SDK.
