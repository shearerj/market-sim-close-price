#!/bin/bash
# Runs experiment where the simulation spec file is already set and emaisl the output

if [ $# -ne 4 ]; then
    echo "Runs an experiment and emails all the results and output"
    echo
    echo "Usage: $0 experiment-name simulation-directory num-obs email"
    exit 1
fi

NAME="$1"
OUTPUT="$NAME.log"
CSV="$NAME.csv"
DIR="$2"
NUM="$3"
EMAIL="$4"

echo "start time: $(date +"%c")" > "$OUTPUT"
./run-hft.sh "$DIR" "$NUM" >> "$OUTPUT"
./obs2csv.py "$DIR/observation"*".json" >> "$OUTPUT"
echo "end time: $(date +"%c")" >> "$OUTPUT"

mail -s "[HFT EXPERIMENT] $NAME" "$EMAIL" < "$OUTPUT"
rm "$OUTPUT"
