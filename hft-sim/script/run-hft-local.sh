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
SCRIPT="$(dirname "$0")"
JAR="$1"
DIR="$2"
NUM="${3:-1}"
NUM_PROC="${4:-1}"

# Observations per process
PER_PROC=$(( $NUM / $NUM_PROC ))

# Run parllely
parfor () { # OUTPUT_FILE START END+1
    for (( OBS = $2; OBS < $3; ++OBS )); do
	echo ">> Running ${DIR%/}: simulation $OBS" >&2
	"$SCRIPT/run-hft-single.sh" "$JAR" "$DIR" "$OBS"
	echo "$DIR/observation$OBS.json" >> "$1"
    done
}

FILES=()
# Don't do the last one to account for rounding
for (( I=1; I<$NUM_PROC; ++I )); do
    FILE=$(mktemp)
    parfor "$FILE" $(( $I * $PER_PROC - $PER_PROC )) $(( $I * $PER_PROC )) &
    FILES+=( "$FILE" )
done
FILE=$(mktemp)
parfor "$FILE" $(( $NUM_PROC * $PER_PROC - $PER_PROC )) $NUM &
FILES+=( "$FILE" )

trap "kill 0" SIGINT SIGTERM #EXIT # Pass signals to children
wait

OBSERVATIONS=()
for FILE in "${FILES[@]}"; do
    OBSERVATIONS+=( $(cat "$FILE") )
    rm "$FILE"
done

# Exit if we got killed
[[ "$NUM" -ne "${#OBSERVATIONS[@]}" ]] && exit 1

# Merging disabled
# if [[ $NUM -gt 1 ]]; then
#     echo -n ">> Merging observations..." >&2
#     "$LOC/merge-obs-egta.py" "${OBSERVATIONS[@]}" > "$FOLDER/merged_observation${NUM}.json"
#     echo " done" >&2
# fi
