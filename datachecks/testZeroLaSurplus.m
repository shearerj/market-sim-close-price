function testResult=testZeroLaSurplus(filename)
% testResult=testZeroSurplus(filename)
%
% Testing whether the latency of data is within range [0, 1000] and whether
%       surplus_sum_total value for latency=0 for each model in the same
%       line is the same.
%
% Example: testZeroLaSurplus('example.mat');
%          (where the data is saved in example.mat)

load(filename);
[datar, datac] = size(data);

testResult = '';
epsilon = 1e-3;

latencyIndex = getIndex(headers, 'latency');
assert(~isempty(latencyIndex), 'Latency column does not exist!');

surpSumTotalIndex = getIndex(headers, 'surplus_sum_total');
[surpr, surpc] = size(surpSumTotalIndex);

logIndex = getIndex(headers, 'obs');


for row=1:datar
    try
    assert(data(row, latencyIndex)<= 1000 && data(row, latencyIndex)>= 0,...
        'Latency out of range at Oberservation %d', ...
        data(row, logIndex));
    catch err
        testResult=strcat(testResult, err.message, '\n');
    end
    if (data(row, latencyIndex) == 0)
        surpSumTotal = data(row, surpSumTotalIndex);
        try
        for j=2:surpr
            assert(le(abs(surpSumTotal(1, j)- surpSumTotal(1, 1)), epsilon), ...
                'Surplus sum total for 0 latency does not match at oberservation %d',...
                data(row, logIndex));  
        end
        catch err
            testResult=strcat(testResult, err.message, '\n');
        end
    end    
end
    


