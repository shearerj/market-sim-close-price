#!/bin/bash
# Run a simulation a given number of times

# Don't know why I set this
IFS='
'

if [[ "$1" == "--help" || "$1" == "-h" ]]; then
    echo "usage: $0 [-h] jar simulation-folder [num-of-obs] [num-proc]"
    echo
    echo "positional arguments:"
    echo " jar          Simulator jar file to use"
    echo " simulation-folder"
    echo "              Folder that contains simulation_spec.json"
    echo " num-of-obs   Num of observations to gather (default: 1)"
    echo " num-proc     Number of processes to use (default: 1)"
    exit 0
elif [ $# -lt 2 ]; then
    echo "usage: $0 [-h] jar simulation-folder [num-of-obs] [num-proc]"
    exit 1
fi

# Parse Arguments
export SCRIPT="$(dirname "$0")"
export JAR="$1"
export DIR="$2"
NUM="${3:-1}"
NUM_PROC="${4:-1}"

function status { # (OBS-NUM)
    echo ">> Running ${DIR%/} $1" >&2
    "$SCRIPT/run-hft-single.sh" "$JAR" "$DIR" "$1"
}

export -f status
seq "$NUM" | xargs -I{} -P"$NUM_PROC" bash -c 'status {}'
