#!/bin/bash

if [[ $# -lt 1 || "$1" == "-h" || "$1" == "--help" ]]; then
    echo "usage: $0 [-h] simulator-name"
    echo
    echo "Creates an EGTA simulator zip named simulator-name using environment configuration defaults specified in <simulator-name>.json. If no <simulator-name>.json is specified, simulator creation will fail. The configuration file must be in the /config directory and must have the same name as your simulator. Note that it will be renamed to defaults.json in the simulator zip." | fold -s
    exit 1
fi

ROOT="$(readlink -m "$(dirname "$0")/..")"
SKELETON="$ROOT/egta"
LIBS="$ROOT/lib"
JAR="$ROOT/dist/hft.jar"
NAME="${1%.zip}"
DEFAULTS="$ROOT/config/$NAME.json"

if [[ -e "$NAME" ]]; then
    echo "Error: Can't create a simulator with the same name as an existing directory"
    exit 1
fi
if [[ ! -e "$JAR" ]]; then
    echo "Error: Can't find hft.jar in /dist directory.  Run build-jar from /build.xml to generate it."
    exit 1
fi
if [[ ! -e "$DEFAULTS" ]]; then
    echo "Error: Can't find $NAME.json in /config directory."
    exit 1
fi

ant
cp -rv "$SKELETON" "$NAME"
cp -v "$DEFAULTS" "$NAME/defaults.json"
mkdir "$NAME/$(basename $LIBS)"
cp -v "$LIBS/"* "$NAME/$(basename $LIBS)"
cp -v "$JAR" "$NAME"
zip -v -r "$NAME" "$NAME"
rm -rf "$NAME"
