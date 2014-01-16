#!/bin/bash
# Executes a different experiment for each sim spec file * presets

IFS='
'
FILE=simulation_spec.json
MERGED=merged
LOGDIR=logs

if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    echo "usage: $0 [-h] directory num-obs [preset preset [preset ...]]"
    echo
    echo "Run simulation spec with all of the specified presets"
    echo
    echo "Creates a new directory for each preset, and runs the spec file num-obs times each, then merges each of the obsevations into a new directory called merged. Logs are also merged and put in the logs director in merged. Presets can be optionally specified after num-obs. If omitted this script will use the default presets of CENTRALCALL, CENTRALCDA, TWOMARKET, and TWOMARKETLA, if included there must be at least two presets" | fold -s
    echo
    echo "example usage:"
    echo "  $0 sim_dir 100"
    echo "  $0 sim_dir 100 CENTRALCDA CENTRALCALL"
    exit 0
elif [[ $# -lt 2 || $# -eq 3 || ! "$2" =~ ^[0-9]+$ ]]; then
    echo "usage: $0 [-h] directory num-obs [preset preset [preset ...]]"
    exit 1
elif [ $# -lt 3 ]; then
    PRESETS=( CENTRALCALL CENTRALCDA TWOMARKET TWOMARKETLA )
else
    PRESETS=( "${@:3}" )
fi

LOC=$(dirname "$0")
FOLDER="$1"
NUM_OBS="$2"

for I in "${!PRESETS[@]}"; do 
    PRESET="${PRESETS[$I]}"
    echo ">> Setting up ${PRESET,,}"
    mkdir -vp "$FOLDER/$PRESET"
    cp -v "$FOLDER/$FILE" "$FOLDER/$PRESET/$FILE"
    sed -i -e 's/"presets" *: *"[^"]*"/"presets": "'"$PRESET"'"/g' -e 's/"modelName" *: *"[^"]*"/"modelName": "'"${PRESET,,}"'"/g' -e 's/"modelNum" *: *"[^"]*"/"modelNum": "'"${I}"'"/g' "$FOLDER/$PRESET/$FILE"
    "$LOC/run-hft.sh" "$FOLDER/$PRESET" "$NUM_OBS"
done

"$LOC/merge-simulation-runs.sh" "$FOLDER/$MERGED" "$NUM_OBS" "${PRESETS[@]/#/$FOLDER/}"
