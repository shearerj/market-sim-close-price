#!/bin/bash
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
NUMSIMS=1000

# Execution
java -Xms1G -Xmx4G -jar "$DIR/target/marketsim-4.0.0-jar-with-dependencies.jar" --egta -s "$1/simulation_spec.json" "$(($NUMSIMS * $2))" \
    | sed -e "1~${NUMSIMS}s#^#[#" -e "0~${NUMSIMS}"'s#$#]#' -e "0~${NUMSIMS}!"'s#$#,#' \
    | jq -c -f "$DIR/jq/merge_payoffs.jq" \
    | split -a4 --additional-suffix=.json -l 1 - "$1/observation_"

# `paste` and `jq` are what do the numsims aggregation. `paste` is used to
# concat NUMSIMS observations on a single line separated by the record
# delimiter. This is then passed into jq which then splits each line into an
# array of json files. This allows the aggregation to be done in parallel.
# Finally, merge_payoffs takes every array and merges it into a single array,
# thus completing the split. The final jq has thw `-c` option putting one
# result on a line before it's split into one line per file with `split`
