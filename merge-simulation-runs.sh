#!/bin/bash
# Executes a different experiment for each sim spec file * presets

# NOTE TO ELAINE
# Feel free to modify this as you like, I probably won't use it.
#
# A change you might want to make is to take arbitrary file paths instead of
# requiring that they all be directories inside a root directory

IFS='
'
FILE=simulation_spec.json
MERGED=merged
LOGDIR=logs

if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    echo "usage: $0 [-h] directory num-obs [folder ...]"
    echo
    echo "Merges several directories with comparable settings e.g. same simulation_spec.json but different presets" | fold -s
    echo
    echo "This utility will create a new folder at <directory>/merged. That folder will be populated with the merged versions of the observations from each of the specified directories. Additionally a directory called <directory>/merged/logs will contain merged versions of the logs from each of those runs. If no folders are specified, this will use every folder in directory isn't names 'merged'." | fold -s
    exit 0
elif [ $# -lt 2 ]; then
    echo "usage: $0 [-h] directory num-obs folder [folder ...]"
    exit 1
elif [ $# -lt 3 ]; then
    # Scan directories and removes one named merged.
    # TODO: Probably a better way to do this
    PRESETS=( $(ls -1F "$1" | grep '/$' | tr -d / | grep -v 'merged') )
else
    PRESETS=( "${@:3}" )
fi

echo "${PRESETS[@]}"

LOC=$(dirname "$0")
FOLDER="$1"
NUM="$2"

mkdir -vp "$FOLDER/$MERGED"
echo -n ">> Merging observations..."

PRE=( "${PRESETS[@]/#/$FOLDER/}" )
for (( OBS=0; OBS < $NUM; ++OBS )); do
    "$LOC/merge-obs-presets.py" "${PRE[@]/%//observation$OBS.json}" > "$FOLDER/$MERGED/observation$OBS.json"
done

# Removed because merge-obs-egta.py doesn't properly handle the renaming of players and config
# OBSERVATIONS=()
# for (( OBS = 0; OBS < $NUM; ++OBS )); do
#     OBSERVATIONS+=( "$FOLDER/$MERGED/observation$OBS.json" )
# done

# "$LOC/merge-obs-egta.py" "${OBSERVATIONS[@]}" > "$FOLDER/$MERGED/observation$((${NUM} - 1))merged.json"

echo " done"

mkdir -vp "$FOLDER/$MERGED/$LOGDIR"
echo -n ">> Merging the log files..."

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
