#!/bin/bash

if [[ "$#" -lt 1 || "$1" == "-h" || "$1" == "--help" ]]; then
    echo "usage: $0 simulator-name"
    exit 1
fi

SKELETON="egta"
LIBS="lib"
JAR="dist/hft.jar"
NAME="$1"

if [ -e "$NAME" ]; then
    echo "Error: Can't create a simulator with the same name as an existing directory"
    exit 1
fi

ant
cp -rv "$SKELETON" "$NAME"
cp -v "$LIBS/"* "$NAME/$LIBS"
cp -v "$JAR" "$NAME"
zip -v -r "$NAME" "$NAME"
rm -rf "$NAME"
