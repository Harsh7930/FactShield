@echo off
setlocal EnableExtensions
rem FactShield — compile all Java sources under src\ into bin\

cd /d "%~dp0"

if not exist "bin\" mkdir bin

echo [build.bat] Compiling to bin\...
javac -encoding UTF-8 -d bin -cp "lib\*" src\dao\NewsDAO.java src\detector\FakeNewsDetector.java src\detector\FakeNewsGUI.java
if errorlevel 1 exit /b 1

echo [build.bat] Done.
