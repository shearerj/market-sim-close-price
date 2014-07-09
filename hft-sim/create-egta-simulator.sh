#!/bin/bash

if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    echo "usage: $0 [-h] simulator-name [defaults.json]"
    echo
    echo "Creates an EGTA simulator zip named simulator-name using the defaults.json specified as defaults.json. If no defaults.json is specified this will use docs/defaults.json. The file for defaults.json can be named anything, it will be renamed to defaults.json in the simulator zip." | fold -s
    exit 1
elif [[ $# -lt 1 ]]; then
    echo "usage: $0 [-h] simulator-name [defaults.json]"
    exit 1
fi

SKELETON="egta"
LIBS="lib"
JAR="dist/hft.jar"
NAME="${1%.zip}"
DEFAULTS="docs/defaults.json"

if [[ -e "$NAME" ]]; then
    echo "Error: Can't create a simulator with the same name as an existing directory"
    exit 1
fi
if [[ ! -e "$JAR" ]]; then
	echo "Error: Can't find hft.jar in /dist directory.  Run build-jar from /build.xml to generate it."
	exit 1
fi
if [[ -n "$2" ]]; then
    DEFAULTS="$2"
fi

ant
cp -rv "$SKELETON" "$NAME"
cp -v "$DEFAULTS" "$NAME/defaults.json"
mkdir "$NAME/$LIBS"
cp -v "$LIBS/"* "$NAME/$LIBS"
cp -v "$JAR" "$NAME"
zip -v -r "$NAME" "$NAME"
rm -rf "$NAME"
