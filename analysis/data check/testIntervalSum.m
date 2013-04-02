function testResult=testIntervalSum(filename)
% testResult=testIntervalSum(filename)
%
% Testing whether transactions_num value for each model in the same
%       line is the same.
%
% Example: testIntervalSum('example.mat');
%          (where the data is saved in example.mat)

load(filename);
[datar, datac] = size(data);

testResult = '';
epsilon = 15000;

intervalSumIndex = getIndex(headers, 'interval_sum');

[transr, transc] = size(intervalSumIndex);

logIndex = getIndex(headers, 'obs');

for row=1:datar
    try
        assert(le(data(row, intervalSumIndex), epsilon), ...
            'Interval sum does not match at oberservation %d',...
            data(row, logIndex));  
    catch err
        testResult=strcat(testResult, err.message, '\n');
    end   
end
    

