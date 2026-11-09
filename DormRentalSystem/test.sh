#!/bin/sh
# Compiles the app and the smoke test, then runs the test on a throw-away data folder.
set -e
cd "$(dirname "$0")"
./build.sh
mkdir -p out-test
javac -d out-test -cp out test/com/dormrental/ServiceSmokeTest.java
java -cp out:out-test com.dormrental.ServiceSmokeTest
