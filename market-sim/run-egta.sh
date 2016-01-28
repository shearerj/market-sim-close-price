#!/usr/bin/env bash
# usage: ./run-egta.sh <directory> <num-obs>
#
# Script to run simulator in the same way the egta will run it. <directory> is
# a directory with a file `simulation_spec.json` containing the single
# simulation spec file, and `num-obs` is the number of observations to create,
# which are created in the same directory.

# Fail if bad stuff happens
set -euf -o pipefail

# Check that appropriate number of parameters is specified
[ "$#" != "2" ] && echo 'Must supply "directory" and "num observations"' 1>&2 && exit 1

# Current directory
DIR="$(dirname "$0")"

# Check if on flux. If so add java 8 to path
if [[ "$(hostname)" == *'arc-ts.umich.edu' ]]; then
    export PATH="/nfs/wellman_ls/bin/:$PATH"
    module load coreutils
fi

# This is the number of observations to condense into one observation
NUMSIMS="$(jq '.configuration.numSims // 1000 | tonumber' "$1/simulation_spec.json")"

# Execution
java -Xms1G -Xmx4G -jar "$DIR/target/marketsim-4.0.0-jar-with-dependencies.jar" --egta -s <(jq 'del(.configuration.numSims)' "$1/simulation_spec.json") "$(($NUMSIMS * $2))" \
    | sed -e "1~${NUMSIMS}s#^#[#" -e "0~${NUMSIMS}"'s#$#]#' -e "0~${NUMSIMS}!"'s#$#,#' \
    | jq -c -f "$DIR/jq/merge_payoffs.jq" \
    | split -a4 --additional-suffix=.json -l 1 - "$1/observation_"

# `sed` and `jq` are what do the numsims aggregation. `sed` is used to turn
# each set of NUMSIMS lines into one json list. This is then passed into jq
# which does the aggregation, and then split, which creates the files. This
# allows the aggregation to be done in parallel.
