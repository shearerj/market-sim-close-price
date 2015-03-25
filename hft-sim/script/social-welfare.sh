#!/bin/bash

if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    echo "usage: $0 <simspec> <role-counts> <profile> <numsamples> <numprocs>"
    exit
elif [[ "$#" -lt "3" ]]; then
    echo "usage: $0 <simspec> <role-counts> <profile> <numsamples> <numprocs>"
    exit 1
fi

SPEC="$1"
ROLECOUNT="$2"
PROFILE="$3"
NSAMP="${4:-1}"
PROCS="${5:-1}"
DIR="$(dirname "$0")"
TMPDIR="$(mktemp -d)" || (echo "couldn't make temporary directory"; exit 1)

"$DIR/sample.py" -s "$SPEC" -r "$ROLECOUNT" -f "$PROFILE" \
    -n "$NSAMP" -d "$TMPDIR"
find "$TMPDIR" -mindepth 1 -maxdepth 1 -type d -print0 \
    | xargs -0n1 -P "$PROCS" "$DIR/run-hft.sh" \
    2>/dev/null
if [[ "$?" -ne "0" ]]; then
    echo "Error running simulations" 1>2
    rm -rf "$TMPDIR"
fi
"$DIR/average-social-welfare.py" "$TMPDIR"/*/observation*.json
rm -rf "$TMPDIR"
