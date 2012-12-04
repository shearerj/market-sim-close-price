#!/bin/bash
# Note: this is for single "player" (i.e. agent with a role)
if [ $# -ne 8 ]
then
echo 'Usage: .\create_spec_file.sh [filename] [# CDA] [# CALL] [call freq] [central mkt on/off] [mkt maker scale factor] [mkt maker sleep] [nbbo_latency]'
exit 1
fi

filename=$1

echo '{ "assignment": { "LA": ["sleepTime_0_alpha_0.001"] },' > $filename
echo '"configuration": { "sim_length": "15000",' >> $filename
echo '"tick_size": "1",' >> $filename
echo '"CDA": "'$2'",' >> $filename
echo '"CALL": "'$3'",' >> $filename
echo '"call_clear_freq": "'$4'",' >> $filename
echo '"central_mkt": "'$5'",' >> $filename
echo '"MARKETMAKER": "1",' >> $filename
echo '"scale_factor": "'$6'",' >> $filename
echo '"sleep_time": "'$7'",' >> $filename
echo '"ZI": "500",' >> $filename
echo '"ZIP": "0",' >> $filename
echo '"nbbo_latency": "'$8'",' >> $filename
echo '"arrival_rate": "0.075",' >> $filename
echo '"mean_PV": "100000",' >> $filename
echo '"kappa": "0.05",' >> $filename
echo '"shock_var": "15000",' >> $filename
echo '"expire_rate": "0.0005",' >> $filename
echo '"bid_range": "2000",' >> $filename
echo '"private_value_var": "10000"' >> $filename
echo '} }' >> $filename
