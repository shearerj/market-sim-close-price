#!/bin/bash
# Executes a different experiment for each sim spec file * presets

if [ $# -lt 2 ]; then
    echo "Run simulation spec with all of the specified presets"
    echo
    echo "Usage: $0 directory num-obs"
    echo
    echo "Creates a new directory for each preset, and runs the spec file num-obs times"
    echo "each, then merges each of the obsevations into a new directory called merged"
    exit 1
fi

PRESETS=( CENTRALCALL CENTRALCDA TWOMARKET TWOMARKETLA )
FILE=simulation_spec.json
MERGED=merged

FOLDER=$1
NUM=$2

for PRESET in ${PRESETS[@]}; do
    echo ">> Setting up ${PRESET,,}"
    mkdir -vp "$FOLDER/$PRESET"
    cp -v "$FOLDER/$FILE" "$FOLDER/$PRESET/$FILE"
    sed -i -e 's/"presets" *: *"[^"]*"/"presets": "'"$PRESET"'"/g' -e 's/"modelName" *: *"[^"]*"/"modelName": "'"${PRESET,,}"'"/g' "$FOLDER/$PRESET/$FILE"
    ./run-hft.sh "$FOLDER/$PRESET" $NUM
done

echo ">> Merging observations..."
mkdir -vp "$FOLDER/$MERGED"

PRE=(${PRESETS[@]/#/"$FOLDER/"})
for (( OBS=0; OBS < $NUM; ++OBS )); do
    ./merge-obs-presets.py ${PRE[@]/%//observation$OBS.json} > "$FOLDER/$MERGED/observation$OBS.json"
done
