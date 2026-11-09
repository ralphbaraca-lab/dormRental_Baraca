#!/bin/sh
# Starts the application. Optional argument: folder for the data files (default: ./data)
cd "$(dirname "$0")"
if [ ! -d out ]; then
  ./build.sh
fi
java -cp out com.dormrental.Main "$@"
