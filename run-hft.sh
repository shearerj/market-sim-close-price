#!/bin/bash
# Run a simulation a given number of times

if [[ $# -lt 1 || "$1" == "--help" || "$1" == "-h" ]]; then
    echo "Usage: $0 simulation-folder [num-of-obs]"
    echo
    echo "    num-of-obs defaults to 1 if not specified"
    exit 1
fi

# Parse Arguments
LOC=$(dirname "$0")
FOLDER=$(readlink -m "$1") # Convert to absolute path
if [[ -z "$2" ]]; then
    NUM=1
else
    NUM="$2"
fi

# Builder
echo ">> Building..."
ant -f "$LOC"
echo ">> Building... done"

# Change to $LOC to run java, necessary for environment properties loading
cd "$LOC"

# Set up class path
CLASSPATH=dist/hft.jar
for i in lib/*.jar; do
  CLASSPATH="${CLASSPATH}:${i}"
done

# Run
OBSERVATIONS=()
for (( OBS = 0; OBS < $NUM; ++OBS )); do
    echo -n ">> Running simulation $OBS..."
    java -cp "${CLASSPATH}" systemmanager.SystemManager "$FOLDER" "$OBS"
    echo " done"
    OBSERVATIONS+=( "$FOLDER/observation$OBS.json" )
done

# Change back after finished running
cd - > /dev/null

if [[ $NUM -gt 1 ]]; then
    echo -n ">> Merging the observations..."
    "$LOC/merge-obs-egta.py" "${OBSERVATIONS[@]}" > "$FOLDER/merged_observation${NUM}.json"
    echo " done"
fi
