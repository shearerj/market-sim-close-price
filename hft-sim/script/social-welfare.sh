#!/bin/bash

if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    echo "usage: $0 <simspec> <profile> <numsamples> <numprocs>"
    exit
elif [[ "$#" -lt "2" ]]; then
    echo "usage: $0 <simspec> <profile> <numsamples> <numprocs>"
    exit 1
fi

PROCS="${4:-1}"
DIR="$(dirname "$0")"
TMPDIR="$(mktemp -d)" || (echo "couldn't make temporary directory"; exit 1)

"$DIR/sample.py" -s "$1" -f "$2" -n "${3:-1}" -d "$TMPDIR"
find "$TMPDIR" -mindepth 1 -maxdepth 1 -type d -print0 \
    | xargs -0n1 -P "${4:-1}" "$DIR/run-hft.sh" \
    2>/dev/null
"$DIR/average-social-welfare.py" "$TMPDIR"/*/observation*.json
rm -rf "$TMPDIR"
