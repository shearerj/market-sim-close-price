#!/bin/bash
# Runs experiments with given parameters. Saves all results in a
# single output CSV file, given by the first input argument.
# Tests for varying latency values

if [ $# -ne 3 ]; then
echo "[REMINDER: Compile jar with ant beforehand]"
echo "Usage: ./experiments_latency.sh [experiment name] [num samples] [email address]"
echo "Example: ./experiments_latency.sh MY_EXPERIMENT 100 ewah@umich.edu"
exit 1
fi

output="$1_output.log"
csv="$1.csv"

echo "start time: $(date +"%c")" > $output

for ((i=0; i<=1000; i+=100)) ;
do
FOLDER="simulations/$1_latency_${i}"

# FOR CALL MARKET CLEAR FREQ = NBBO UPDATE LATENCY
if [ ! -d "$FOLDER" ]; then
   mkdir "$FOLDER"
else
   rm -rf $FOLDER/*
fi
./create_spec_file.sh "${FOLDER}/simulation_spec.json" $i
echo "Samples for ${FOLDER}" >> $output
./run_hft.sh "${FOLDER}" $2 >> $output
echo "Parsing" >> $output
./parse_single.sh $csv ${FOLDER} >> $output

done # end of latency for loop

echo "end time: $(date +"%c")" >> $output
mail -s "[HFT EXPERIMENT] $1 " "$3" < $output
rm $output
