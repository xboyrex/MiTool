@echo off
title MiTool + MiUnlockTool by xboyrex — PC Desktop GUI
color 0C
cd /d "%~dp0"
python -m mitool.gui
if errorlevel 1 pause
