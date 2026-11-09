@echo off
rem Compiles the application into the "out" folder
cd /d "%~dp0"
if not exist out mkdir out
javac -d out -sourcepath src src\com\dormrental\Main.java
if errorlevel 1 (
    echo Build FAILED.
    exit /b 1
)
echo Build complete. Start the app with run.bat
