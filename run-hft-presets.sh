#!/bin/bash
# Executes a different experiment for each sim spec file * presets

if [ $# -lt 2 ]; then
    echo "Run simulation spec with all of the specified presets"
    echo
    echo "Usage: $0 directory num-obs [preset1]..."
    echo
    echo "Creates a new directory for each preset, and runs the spec file num-obs times"
    echo "each, then merges each of the obsevations into a new directory called merged."
    echo "Presets can be optionally specified after num-obs. If omitted this script will"
    echo "use the default presets of CENTRALCALL, CENTRALCDA, TWOMARKET, and TWOMARKETLA."
    exit 1
elif [ $# -lt 3 ]; then
    PRESETS=( CENTRALCALL CENTRALCDA TWOMARKET TWOMARKETLA )
else
    PRESETS=( "${@:3}" )
fi
FILE=simulation_spec.json
MERGED=merged

FOLDER=$1
NUM=$2

for I in "${!PRESETS[@]}"; do 
    PRESET="${PRESETS[$I]}"
    echo ">> Setting up ${PRESET,,}"
    mkdir -vp "$FOLDER/$PRESET"
    cp -v "$FOLDER/$FILE" "$FOLDER/$PRESET/$FILE"
    sed -i -e 's/"presets" *: *"[^"]*"/"presets": "'"$PRESET"'"/g' -e 's/"modelName" *: *"[^"]*"/"modelName": "'"${PRESET,,}"'"/g' -e 's/"modelNum" *: *"[^"]*"/"modelNum": "'"${I}"'"/g' "$FOLDER/$PRESET/$FILE"
    ./run-hft.sh "$FOLDER/$PRESET" $NUM
done

echo ">> Merging observations..."
mkdir -vp "$FOLDER/$MERGED"

PRE=(${PRESETS[@]/#/"$FOLDER/"})
for (( OBS=0; OBS < $NUM; ++OBS )); do
    ./merge-obs-presets.py ${PRE[@]/%//observation$OBS.json} > "$FOLDER/$MERGED/observation$OBS.json"
done
