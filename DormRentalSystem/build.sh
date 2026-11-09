#!/bin/sh
# Compiles the application into ./out
set -e
cd "$(dirname "$0")"
mkdir -p out
javac -d out -sourcepath src src/com/dormrental/Main.java
echo "Build complete. Start the app with ./run.sh"
