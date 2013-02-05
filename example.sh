#!/bin/bash
# This script will generate a CSV file of various metrics from running the simulation.

if [ $# -ne 4 ]
then
echo "REMINDER: Compile with ant before running!"
echo "Usage: ./example.sh [HFT type] [NBBO latency] [# samples] [CSV filename]"
echo "Example: ./example.sh LA 0 10 test.csv"
exit 1
fi

strat="sleepTime_0_alpha_0.001"
baseDir="simulations/"

folder="${baseDir}${1}_latency_${2}"

# LOG START TIME
echo "start time: $(date +"%c")"

if [ ! -d "$folder" ]; then
   mkdir "$folder"
else
   rm -rf $folder/*
fi
./create_spec_file.sh "${folder}/simulation_spec.json" $1 ${strat} $2
./run_hft.sh ${folder} $3
./parse_single.sh $4 ${folder}


# LOG END TIME
echo "end time: $(date +"%c")"
