#!/bin/bash
# Runs experiment where the simulation spec file is already set
if [ $# -ne 4 ]; then
echo "Usage: ./basic_run.sh [Experiment name] [Directory of observations] [# samples] [email address]"
exit 1
fi

output="$1.log"
csv="$1.csv"

echo "start time: $(date +"%c")" > $output
./run_hft.sh $2 $3 >> $output
./parse_single.sh $csv $2 >> $output
echo "end time: $(date +"%c")" >> $output

mail -s "[HFT EXPERIMENT] $1 " "$4" < $output
rm $output
