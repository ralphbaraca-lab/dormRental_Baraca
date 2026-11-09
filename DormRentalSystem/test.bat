@echo off
rem Compiles the app and the smoke test, then runs the test on a throw-away data folder.
cd /d "%~dp0"
call build.bat
if errorlevel 1 exit /b 1
if not exist out-test mkdir out-test
javac -d out-test -cp out test\com\dormrental\ServiceSmokeTest.java
if errorlevel 1 exit /b 1
java -cp out;out-test com.dormrental.ServiceSmokeTest
