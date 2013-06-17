#!/bin/bash
# Note: this is for running two 2M models (with and without LA) plus 1 each centralized CDA, CALL markets.
if [ $# -ne 2 ]
then
echo 'Usage: ./create_spec_file.sh [filename] [NBBO update latency]'
exit 1
fi

filename=$1

echo '{ "assignment": {},' > $filename
echo '"configuration": { "sim_length": "15000",' >> $filename
echo '"primary_model": "TWOMARKET-LA",' >> $filename
echo '"TWOMARKET": "LA:sleepTime_0_alpha_0.001,DUMMY",' >> $filename
echo '"CENTRALCDA": "1",' >> $filename
echo '"CENTRALCALL": "NBBO",' >> $filename
echo '"BASICMM": "0",' >> $filename
echo '"BASICMM_setup": "",' >> $filename
echo '"ZI": "250",' >> $filename
echo '"ZI_setup": "bidRange_2000",' >> $filename
echo '"ZIP": "0",' >> $filename
echo '"ZIP_setup": "",' >> $filename
echo '"AA": "0",' >> $filename
echo '"AA_setup": "",' >> $filename
echo '"tick_size": "1",' >> $filename
echo '"nbbo_latency": "'$2'",' >> $filename
echo '"arrival_rate": "0.075",' >> $filename
echo '"reentry_rate": "0.005",' >> $filename
echo '"mean_value": "100000",' >> $filename
echo '"kappa": "0.05",' >> $filename
echo '"shock_var": "150000000",' >> $filename
echo '"private_value_var": "100000000",' >> $filename
echo '} }' >> $filename
