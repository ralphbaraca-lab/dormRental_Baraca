@echo off
rem Starts the application. Optional argument: folder for the data files (default: data)
cd /d "%~dp0"
if not exist out call build.bat
java -cp out com.dormrental.Main %*
