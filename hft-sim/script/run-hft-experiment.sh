#!/bin/bash
# Allows running arbitrary simulations try running with --help

if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    echo "usage: $0 [-h] directory num-obs num-procs jpath values [jpath values ...]"
    echo
    echo "Run an experiment with modifications of the simulation_spec"
    echo
    echo "\"directory\" must have a valid simulation_spec.json in it. Each jpath specified reprsents a dimension of the simulation spec to modify, and the space separated values represents all of the grid points to sample. By specifying multiple jpath values pairs, one can do a multidimensional grid search over specification parameters." | fold -s
    echo
    echo "example usage:"
    echo "  $0 sim_dir 100 presets \"CENTRALCDA TWOMARKET\" nbboLatency \"5 10 20 50 100 200\""
    echo "  # This will simulate various nbbo latencies for both a central cda and"
    echo "  # a two market model"
    exit 0
elif [[ "$#" -lt 5 || "$(( $# % 2 ))" -ne 1 ]]; then
    echo "usage: $0 [-h] directory num-obs jpath values [jpath values ...]"
    exit 1
fi

# defaults (e.g. by default one can only modify the configuration parameters)
SPEC_FILE="simulation_spec.json"
DEFAULT_JPATH="configuration"

# Get first couple of variables
LOC="$(dirname "$0")"
DIR="$1"
NUM_OBS="$2"
NUM_PROCS="$3"
SPEC="$(< "$DIR/$SPEC_FILE")" # simulation_spec in a bash variable

# Set up for doing the grid search over parameters
ARGS=( "${@:4}" ) # All of the grid search arguments
NARGS=$(( ${#ARGS[@]} / 2 )) # Number of pairs
INDEX=( $( yes 0 | head -n "$NARGS" ) ) # The current index for the grid for each pair
LENGTH=( $( yes 0 | head -n "$NARGS" ) ) # The total number of values in each pair
for (( I=0; I < $NARGS; ++I )); do
    VALS=( ${ARGS[$(( 2 * $I + 1 ))]} )
    LENGTH[$I]=${#VALS[@]}
done

# Run the grid search. Terminate when the index of the first element is out of bounds
while [[ ${INDEX[0]} -lt ${LENGTH[0]} ]]; do
    # Set the file name
    NAME=""
    # For the current set of indices append the settings to the directory name
    # Also use jpath -v to set the appropriate field for the current indices
    for (( I=0; I < $NARGS; ++I )); do
        JPATH="${ARGS[$(( 2 * $I ))]}"
        VALS=( ${ARGS[$(( 2 * $I + 1 ))]} )
        VAL=${VALS[${INDEX[$I]}]}
        NAME="${NAME}_${JPATH// /-}_$VAL"
        SPEC="$( "$LOC/jpath.py" $DEFAULT_JPATH $JPATH -v $VAL <<< "$SPEC" )"
    done

    # Create the experiment name directory (remove the preciding underscore from NAME)
    EXP_DIR="$DIR/${NAME#_}"
    # Status message
    echo ">>> Creating Experiment \"$EXP_DIR\""
    # Create directory
    mkdir -pv "$EXP_DIR"
    # Save spec in directory
    echo "$SPEC" > "$EXP_DIR/$SPEC_FILE"
    # Run simulation
    "$LOC/run-hft.sh" "$EXP_DIR" "$NUM_OBS" "$NUM_PROCS"

    # Incriment the index for the last value pair
    INDEX[$(( $NARGS - 1 ))]=$(( ${INDEX[$(( $NARGS - 1 ))]} + 1 ))
    # Iterate backwards fro mthe last pair, resetting every wrapped pair, and
    # then incriment its preceeding index
    for (( I=$(( $NARGS - 1 )); I > 0; I-- )); do
        if [[ ${INDEX[$I]} -eq ${LENGTH[$I]} ]]; then
            INDEX[$I]=0
            INDEX[$(( $I - 1 ))]=$(( ${INDEX[$(( $I - 1 ))]} + 1 ))
        else
            break # Invariante that no earlier index could have exceeded length
        fi
    done
done
