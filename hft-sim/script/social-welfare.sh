#!/bin/bash

if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    echo "usage: $0 <simspec> <role-counts> <profile> <numsamples>"
    echo "           <numprocs> [jar]"
    exit
elif [[ "$#" -lt "3" ]]; then
    echo "usage: $0 <simspec> <role-counts> <profile> <numsamples>"
    echo "           <numprocs> [jar]"
    exit 1
fi

SPEC="$1"
ROLECOUNT="$2"
PROFILE="$3"
NSAMP="${4:-1}"
PROCS="${5:-1}"
DIR="$(dirname "$0")"
TMPDIR="$(mktemp -d)" || (echo "couldn't make temporary directory"; exit 1)
if [[ -z "$6" ]]; then
    EXEC=("$DIR/run-hft.sh")
else
    EXEC=("$DIR/run-hft-local.sh" "$6")
fi

"$DIR/sample.py" -s "$SPEC" -r "$ROLECOUNT" -f "$PROFILE" \
    -n "$NSAMP" -d "$TMPDIR"
find "$TMPDIR" -mindepth 1 -maxdepth 1 -type d -print0 \
    | xargs -0n1 -P"$PROCS" "${EXEC[@]}"
"$DIR/average-social-welfare.py" "$TMPDIR"/*/observation*.json
rm -rf "$TMPDIR"
