#!/bin/bash
# Note: this is for single "player" (i.e. agent with a role)
if [ $# -ne 8 ]
then
echo 'Usage: .\create_spec_file.sh [filename] [HFT type] [HFT strat] [# CDA] [# CALL] [call freq] [central mkt on/off] [nbbo_latency]'
exit 1
fi

filename=$1

echo '{ "assignment": { "'$2'": ["'$3'"] },' > $filename
echo '"configuration": { "sim_length": "15000",' >> $filename
echo '"tick_size": "1",' >> $filename
echo '"CDA": "'$4'",' >> $filename
echo '"CALL": "'$5'",' >> $filename
echo '"call_clear_freq": "'$6'",' >> $filename
echo '"central_mkt": "'$7'",' >> $filename
echo '"MARKETMAKER": "1"', >> $filename
echo '"ZI": "500",' >> $filename
echo '"ZIP": "2",' >> $filename
echo '"nbbo_latency": "'$8'",' >> $filename
echo '"arrival_rate": "0.075",' >> $filename
echo '"mean_PV": "100000",' >> $filename
echo '"kappa": "0.05",' >> $filename
echo '"shock_var": "15000",' >> $filename
echo '"expire_rate": "0.0005",' >> $filename
echo '"bid_range": "2000",' >> $filename
echo '"private_value_var": "10000"' >> $filename
echo '} }' >> $filename
