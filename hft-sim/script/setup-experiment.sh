#!/bin/bash
# Allows running arbitrary simulations try running with --help

if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    echo "usage: $0 [-h] directory jpath values [jpath values ...]"
    echo
    echo "Run an experiment with modifications of the simulation_spec"
    echo
    echo "\"directory\" must have a valid simulation_spec.json in it. Each jpath specified represents a parameter of the simulation spec to modify in parallel, and the space separated values represents all of the values to set the parameter to. By specifying multiple jpath values pairs, one can vary several parameters together." | fold -s
    echo
    echo "example usage:"
    echo "  $0 sim_dir nbboLatency \"5 10 20 50 100 200\""
    echo "  # This will create directories to simulate various nbbo latencies for"
    echo "  # both a central cda and a two market model"
    echo "  ls -d sim_dir/*/ | xargs -I{} ./script/run-hft.sh {} num-obs num-proc"
    echo "  # This will run all of the newly created simulations"
    exit 0
elif [[ "$#" -lt 3 || "$(( $# % 2 ))" -ne 1 ]]; then
    echo "usage: $0 [-h] directory jpath values [jpath values ...]"
    exit 1
fi

# defaults (e.g. by default one can only modify the configuration parameters)
SPEC_FILE="simulation_spec.json"
DEFAULT_JPATH="configuration"

# Get first couple of variables
LOC="$(dirname "$0")"
DIR="$1"
SPEC="$(< "$DIR/$SPEC_FILE")" # simulation_spec in a bash variable

# Set up for doing the grid search over parameters
ARGS=( "${@:2}" ) # All of the grid search arguments
NARGS=$(( ${#ARGS[@]} / 2 )) # Number of pairs
N=( ${ARGS[1]} )
N=${#N[@]}

# Run the grid search. Terminate when the index of the first element is out of bounds
for (( J=0; J < $N; ++J )); do
    # Set the file name
    NAME=""
    # For the current set of indices append the settings to the directory name
    # Also use jpath -v to set the appropriate field for the current indices
    for (( I=0; I < $NARGS; ++I )); do
        JPATH="${ARGS[$(( 2 * $I ))]}"
        VALS=( ${ARGS[$(( 2 * $I + 1 ))]} )
        VAL=${VALS[$J]}
        NAME="${NAME}_${JPATH// /-}_$VAL"
        SPEC="$( "$LOC/jpath.py" $DEFAULT_JPATH $JPATH -v $VAL <<< "$SPEC" )"
    done
    # Strip non word characters
    NAME="$(tr -dc '[:alnum:][_]' <<< "$NAME")"

    # Create the experiment name directory (remove the preciding underscore from NAME)
    EXP_DIR="$DIR/${NAME#_}"
    # Create directory
    mkdir -pv "$EXP_DIR"
    # Save spec in directory
    echo "$SPEC" > "$EXP_DIR/$SPEC_FILE"
done
