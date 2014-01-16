#!/bin/bash
# Executes a different experiment for each sim spec file * presets

IFS='
'
FILE=simulation_spec.json
MERGED=merged
LOGDIR=logs

if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    echo "usage: $0 [-h] directory num-obs [max-latency [latency-step]]"
    echo
    echo "Run simulation spec with various slow LA latencies"
    exit 0
elif [ $# -lt 2 ]; then
    echo "usage: $0 [-h] directory num-obs [max-latency [latency-step]]"
    exit 1
fi

LOC=$(dirname "$0")
FOLDER="$1"
NUM="$2"
LAT_MAX=50
LAT_STEP=10

if [ $# -ge 3 ]; then
    LAT_MAX="$3"
fi
if [ $# -ge 4 ]; then
    LAT_STEP="$4"
fi

#TODO Read simspec file, and modify nbbo and market latency if too low

for (( LAT=$LAT_STEP; LAT<=$LAT_MAX; LAT+=$LAT_STEP )); do
    echo ">> Setting up latency $LAT"
    mkdir -vp "$FOLDER/$LAT"
    cp -v "$FOLDER/$FILE" "$FOLDER/$LAT/$FILE"
    sed -i -e 's/"LA:laLatency_0"/"LA:laLatency_'"${LAT}"'"/g' -e 's/"modelName" *: *"[^"]*"/"modelName": "'"lat${LAT}"'"/g' -e 's/"modelNum" *: *"[^"]*"/"modelNum": "'"${LAT}"'"/g' "$FOLDER/$LAT/$FILE"
    "$LOC/run-hft.sh" "$FOLDER/$LAT" $NUM
done
