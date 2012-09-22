#!/bin/bash
# Note: this is for single "player" (i.e. agent with a role)
if [ $# -ne 7 ]
then
echo 'Usage: .\create_spec_file.sh [filename] [HFT type] [HFT strat] [num_cda] [num_call] [call_freq] [nbbo_latency]'
exit 1
fi

filename=$1

echo '{ "assignment": { "'$2'": ["'$3'"] },' > $filename
echo '"configuration": { "sim_length": "75000",' >> $filename
echo '"tick_size": "100",' >> $filename
echo '"CDA": "'$4'",' >> $filename
echo '"CALL": "'$5'",' >> $filename
echo '"call_clear_freq": "'$6'",' >> $filename
echo '"BACKGROUND": "500",' >> $filename
echo '"nbbo_latency": "'$7'",' >> $filename
echo '"arrival_rate": "0.75",' >> $filename
echo '"mean_PV": "50000",' >> $filename
echo '"kappa": "0.01",' >> $filename
echo '"shock_var": "100",' >> $filename
echo '"expire_rate": "0.01",' >> $filename
echo '"bid_range": "2000",' >> $filename
echo '"value_var": "100"' >> $filename
echo '} }' >> $filename
