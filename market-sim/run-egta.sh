#!/bin/bash
# usage: ./run-egta.sh <directory> <num-obs>
#
# Script to run simulator in the same way the egta will run it. <directory> is
# a directory with a file `simulation_spec.json` containing the single
# simulation spec file, and `num-obs` is the number of observations to create,
# which are created in the same directory.

# Fail if bad stuff happens
set -euf -o pipefail

# Chec that appropriate number of parameters is specified
[ "$#" != "2" ] && echo 'Must supply "directory" and "num observations"' 1>&2 && exit 1

# Current directory
DIR="$(dirname "$0")"

# Check if on flux. If so add java 8 to path
if [[ "$(hostname)" == *'arc-ts.umich.edu' ]]; then
    export PATH="/nfs/wellman_ls/bin/:$PATH"
    module load coreutils
fi

# Execution
java -jar "$DIR/target/marketsim-4.0.0-jar-with-dependencies.jar" --egta -s "$1/simulation_spec.json" "$2" | split -a4 --additional-suffix=.json -l 1 - "$1/observation_"
