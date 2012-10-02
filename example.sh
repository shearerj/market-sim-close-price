#!/bin/bash
# This script will generate a CSV file of various metrics from running the simulation.

if [ $# -ne 4 ]
then
echo "Usage: .\example.sh [market type] [NBBO latency] [# samples] [CSV filename]"
exit 1
fi

strat="sleepTime_0_alpha_0.001"
baseDir="simulations/"

folder="${baseDir}${1}_latency_${2}"

# LOG START TIME
echo "start time: $(date +"%c")"

# CDA MARKET
if [ "$1" == "cda" ]; then

if [ ! -d "$folder" ]; then
   mkdir "$folder"
fi
./create_spec_file.sh "${folder}/simulation_spec.json" LA ${strat} 2 0 10 "on" $2
./run_hft.sh ${folder} $3
./parse_single.sh $4 ${folder}
fi


# CALL MARKET
if [ "$1" == "call" ]; then

clearFreq=50
newfolder="${folder}_clear_${clearFreq}"a

if [ ! -d "$newfolder" ]; then
   mkdir "$newfolder"
fi
./create_spec_file.sh "${newfolder}/simulation_spec.json" LA ${strat} 0 2 ${clearFreq} "on" $2
./run_hft.sh ${newfolder} $3
./parse_single.sh $4 ${folder}
fi


# LOG END TIME
echo "end time: $(date +"%c")"
