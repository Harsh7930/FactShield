@echo off
setlocal EnableExtensions
rem FactShield — build then run from project root (ai_detector.py via user.dir)

cd /d "%~dp0"

call build.bat
if errorlevel 1 exit /b 1

echo [run.bat] Starting FactShield UI...
java -cp "bin;lib\*" detector.FakeNewsGUI
