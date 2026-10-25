@echo off
title MiTool + MiUnlockTool by xboyrex — Standalone Windows EXE Builder
color 0C
echo ============================================================================
echo        X B O Y R E X   M I T O O L   W I N D O W S   E X E   B U I L D E R
echo               Developed by xboyrex (Mayank Droid) - v3.0 Unified
echo ============================================================================
echo.
cd /d "%~dp0"
pip install -U pyinstaller customtkinter rich requests pyaes migate miunlock miflash miasst fcetool miapply
pyinstaller --noconfirm --onefile --windowed --icon="assets\app_icon.ico" --add-data "mitool;mitool" --add-data "miunlock;miunlock" --name "MiTool_by_xboyrex_v3.0" mitool\gui.py
echo.
echo [✔] SUCCESS! Standalone Windows EXE created at:
echo     %~dp0dist\MiTool_by_xboyrex_v3.0.exe
pause
