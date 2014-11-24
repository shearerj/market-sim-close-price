#!/bin/bash
# Email the result and time of a script

if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    echo "usage: $0 [-h] experiment-name email command [arg [arg ...]]"
    echo
    echo "Runs a script and emails all the results and output"
    echo
    echo "examples:"
    echo "  $0 test-experiment me@website.com ./script/run-hft.sh sim-folder 10"
    echo "  $0 test-experiment me@website.com bash -c './script/run-hft.sh sim-folder 10; ./script/obs2csv sim-folder/observation*.json'"
    exit 0
elif [ $# -le ]; then
    echo "usage: $0 [-h] experiment-name email command [arg [arg ...]]"
    exit 1
fi

NAME="$1"
EMAIL="$2"
OUTPUT="$NAME.log"

echo "start time: $(date +"%c")" > "$OUTPUT"
${@:3} >> "$OUTPUT" 2>&1
echo "end time: $(date +"%c")" >> "$OUTPUT"

mail -s "[HFT EXPERIMENT] $NAME" "$EMAIL" < "$OUTPUT"
rm "$OUTPUT"
