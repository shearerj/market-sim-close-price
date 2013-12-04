#!/bin/bash
# Run a simulation a given number of times

if [ $# -lt 1 ]; then
    echo "Usage: $0 simulation-folder [num-of-obs]"
    echo
    echo "    num-of-obs defaults to 1 if not specified"
    exit 1
fi

# Parse Arguments
FOLDER="$1"
if [[ -z "$2" ]]; then
    NUM=1
else
    NUM="$2"
fi

# Builder
echo ">> Building..."
ant
echo ">> Building... done"

# Set up class path
CLASSPATH=dist/hft.jar
for i in lib/*.jar; do
  CLASSPATH="${CLASSPATH}:${i}"
done

# Run
for (( OBS = 0; OBS < $NUM; ++OBS )); do
    echo -n ">> Running simulation $OBS..."
    java -cp "${CLASSPATH}" systemmanager.SystemManager "$FOLDER" "$OBS"
    echo " done"
done
