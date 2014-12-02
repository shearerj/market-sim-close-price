#!/bin/bash
# Script to more easily envoke the simulator

if [[ "$1" == "--help" || "$1" == "-h" ]]; then
    echo "usage: $0 [-h] jar simulation-folder observation-num"
    echo
    echo "positional arguments:"
    echo " jar          Simulator jar file to use"
    echo " simulation-folder"
    echo "              Folder that contains simulation_spec.json"
    echo " observation-num"
    echo "              Observation number to run"
    exit 0
elif [ $# -lt 3 ]; then
    echo "usage: $0 [-h] jar simulation-folder observation-num"
    exit 1
fi

ROOT="$(readlink -m "$(dirname "$0")/..")"
JAR="$(realpath "$1" --relative-to "$ROOT")"
DIR="$(realpath "$2" --relative-to "$ROOT")"

cd "$ROOT" # Move to root for java to read settings appropriately

CLASSPATH="$(ls "lib/"*.jar | tr '\n' :)$JAR"
java -cp "${CLASSPATH}" systemmanager.SystemManager "$DIR" "$3"

cd - > /dev/null # CD back
