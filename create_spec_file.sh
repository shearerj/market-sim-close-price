#!/bin/bash
# Note: this is for running two 2M models (with and without LA) plus 1 each centralized CDA, CALL markets.
# Primary model is the 2M with LA.
if [ $# -ne 4 ]
then
echo 'Usage: .\create_spec_file.sh [filename] [HFT type] [HFT strat] [NBBO update latency]'
exit 1
fi

filename=$1

echo '{ "assignment": { "'$2'": ["'$3'"], "DUMMY": [""]  },' > $filename
echo '"configuration": { "sim_length": "15000",' >> $filename
echo '"tick_size": "1",' >> $filename
echo '"primary_model": "TWOMARKET-LA",' >> $filename
echo '"TWOMARKET": "LA,DUMMY",' >> $filename
echo '"CENTRALCDA": "1",' >> $filename
echo '"CENTRALCALL": "NBBO",' >> $filename
echo '"MARKETMAKER": "0",' >> $filename
echo '"ZI": "250",' >> $filename
echo '"ZIP": "0",' >> $filename
echo '"nbbo_latency": "'$4'",' >> $filename
echo '"arrival_rate": "0.075",' >> $filename
echo '"mean_PV": "100000",' >> $filename
echo '"kappa": "0.05",' >> $filename
echo '"shock_var": "15000",' >> $filename
echo '"expire_rate": "0.0005",' >> $filename
echo '"bid_range": "2000",' >> $filename
echo '"private_value_var": "10000"' >> $filename
echo '} }' >> $filename
