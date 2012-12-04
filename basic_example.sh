#!/bin/bash
# This script will run the specified number of simulations for the two-market model at a given latency.
# Note: Do not include a slash at the end of the simulation folder name (which is the directory containing the simulation_spec.json file)
#
# Example: .\basic_example.sh simulations/1 100 1
if [ $# -ne 3 ]
then
echo "Usage: .\basic_example.sh [simulation folder] [NBBO latency] [# simulations]"
exit 1
fi

clearFreq=10
folder=${1%/}

mmScaleFactor=10
mmSleepTime=100

./create_spec_file.sh "${folder}/simulation_spec.json" 2 0 ${clearFreq} "on" ${mmScaleFactor} ${mmSleepTime} $2
./run_hft.sh ${folder} $3
