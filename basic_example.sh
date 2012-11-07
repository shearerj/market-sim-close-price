#!/bin/bash
# This script will run one simulation for the two-market model at a given latency.

if [ $# -ne 2 ]
then
echo "Usage: .\basic_example.sh [HFT type] [NBBO latency]"
exit 1
fi

strat="sleepTime_0_alpha_0.001"
baseDir="simulations/"

folder="${baseDir}${1}_latency_${2}"


if [ ! -d "$folder" ]; then
   mkdir "$folder"
fi
./create_spec_file.sh "${folder}/simulation_spec.json" $1 ${strat} 2 0 10 "on" $2
./run_hft.sh ${folder} 1
