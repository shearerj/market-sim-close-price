function testResult=testLAtransaction(filename)
% testResult=testLAtransaction(filename)
%
% Testing whether transactions_num value for latency=0 for each model in the same
%       line is the same.
%
% Example: testLAtransaction('example.mat');
%          (where the data is saved in example.mat)

load(filename);
[datar, datac] = size(data);

testResult = '';
epsilon = 1e-3;

latencyIndex = getIndex(headers, 'latency');

transactionNumIndex = getIndex(headers, 'transactions_num');
assert(~isempty(transactionNumIndex), 'Transaction num column does not exist!');

[transr, transc] = size(transactionNumIndex);

logIndex = getIndex(headers, 'obs');

for row=1:datar
    try
        if data(row, latencyIndex) == 0
        for j=2:transr-1
            assert(le(abs(data(row, transactionNumIndex(j)) - data(row, transactionNumIndex(1))), epsilon), ...
                'Transaction number for latency 0 does not match at oberservation %d',...
                data(row, logIndex));  
        end
        end
    catch err
        testResult=strcat(testResult, err.message, '\n');
    end   
end
    

