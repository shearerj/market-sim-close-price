#!/bin/bash
# This script will generate a CSV file of various metrics from running the simulation.

if [ $# -ne 2 ]
then
echo "Usage: .\basic_example.sh [market type] [NBBO latency]"
exit 1
fi

strat="sleepTime_0_alpha_0.001"
baseDir="simulations/"

folder="${baseDir}${1}_latency_${2}"


# CDA MARKET
if [ "$1" == "cda" ]; then

if [ ! -d "$folder" ]; then
   mkdir "$folder"
fi
./create_spec_file.sh "${folder}/simulation_spec.json" LA ${strat} 2 0 10 $2
./run_hft.sh ${folder} 1
fi


# CALL MARKET
if [ "$1" == "call" ]; then

newfolder="${folder}"
clearFreq=75

if [ ! -d "$newfolder" ]; then
   mkdir "$newfolder"
fi
./create_spec_file.sh "${newfolder}/simulation_spec.json" LA ${strat} 0 2 ${clearFreq} $2
./run_hft.sh ${newfolder} 1
fi

