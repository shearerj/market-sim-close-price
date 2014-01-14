#!/bin/bash
# Executes a different experiment for each sim spec file * presets

if [ $# -lt 2 ]; then
    echo "Run simulation spec with all of the specified presets"
    echo
    echo "Usage: $0 directory num-obs preset [preset ...]"
    echo
    echo "Creates a new directory for each preset, and runs the spec file num-obs times"
    echo "each, then merges each of the obsevations into a new directory called merged."
    echo "Logs are also merged and put in the logs director in merged. Presets can be"
    echo "optionally specified after num-obs. If omitted this script will use the default"
    echo "presets of CENTRALCALL, CENTRALCDA, TWOMARKET, and TWOMARKETLA."
    exit 1
elif [ $# -lt 3 ]; then
    PRESETS=( CENTRALCALL CENTRALCDA TWOMARKET TWOMARKETLA )
else
    PRESETS=( "${@:3}" )
fi
FILE=simulation_spec.json
MERGED=merged
LOGDIR=logs

LOC=$(dirname "$0")
FOLDER="$1"
NUM="$2"

for I in "${!PRESETS[@]}"; do 
    PRESET="${PRESETS[$I]}"
    echo ">> Setting up ${PRESET,,}"
    mkdir -vp "$FOLDER/$PRESET"
    cp -v "$FOLDER/$FILE" "$FOLDER/$PRESET/$FILE"
    sed -i -e 's/"presets" *: *"[^"]*"/"presets": "'"$PRESET"'"/g' -e 's/"modelName" *: *"[^"]*"/"modelName": "'"${PRESET,,}"'"/g' -e 's/"modelNum" *: *"[^"]*"/"modelNum": "'"${I}"'"/g' "$FOLDER/$PRESET/$FILE"
    "$LOC/run-hft.sh" "$FOLDER/$PRESET" $NUM
done

# Removed because merge-obs-egta.py doesn't properly handle the renaming of players and config
# echo -n ">> Merging preset observations..."
# mkdir -vp "$FOLDER/$MERGED"

# PRE=(${PRESETS[@]/#/"$FOLDER/"})
# for (( OBS=0; OBS < $NUM; ++OBS )); do
#     "$LOC/merge-obs-presets.py" ${PRE[@]/%//observation$OBS.json} > "$FOLDER/$MERGED/observation$OBS.json"
# done

# OBSERVATIONS=()
# for (( OBS = 0; OBS < $NUM; ++OBS )); do
#     OBSERVATIONS+=( "$FOLDER/$MERGED/observation$OBS.json" )
# done

# "$LOC/merge-obs-egta.py" "${OBSERVATIONS[@]}" > "$FOLDER/$MERGED/observation$((${NUM} - 1))merged.json"
# echo " done"

echo -n ">> Merging the log files..."
mkdir -vp "$FOLDER/$MERGED/$LOGDIR"

NUMSIMS=$( cat "$FOLDER/$FILE" | grep -Eo '"numSims" *: *"[0-9]+"' | grep -Eo '[0-9]+' )

for (( OBS=0; OBS < $NUM; ++OBS )); do
    for (( SIM=0; SIM < $NUMSIMS; ++SIM )); do
	LOGS=()
	for PRESET in "${PRESETS[@]}"; do
	    LOGS+=( $( ls "$FOLDER/$PRESET/$LOGDIR/"*"_${PRESET}_${OBS}_${SIM}_"*".txt" | sort | tail -n1 ) )
	done
	"$LOC/merge-logs.py" "${LOGS[@]}" > "$FOLDER/$MERGED/$LOGDIR/$( echo $FOLDER/$MERGED | tr '/' '_' | tr -d '.' | sed 's/__*/_/g' )_${OBS}_${SIM}_$( date '+%Y-%m-%d-%H-%M-%S' ).txt"
    done
done
echo " done"
