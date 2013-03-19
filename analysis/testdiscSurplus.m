function testResult=testdiscSurplus(filename)
% testResult=testdiscSurplus(filename)
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

ccda1Index = getIndex(headers, 'centralcda1_surplus_disc');
twomarketdummyIndex = getIndex(headers, 'twomarketdummy_surplus_disc');
twomarketlaIndex = getIndex(headers, 'twomarketla_surplus_disc');

[surpr, surpc] = size(ccda1Index);


logIndex = getIndex(headers, 'obs');

for row=1:1%datar
    try
        if data(row, latencyIndex) == 0
        for entry = 1:surpr
            assert(le(abs(data(row, ccda1Index(entry)) - data(row, twomarketdummyIndex(entry))), epsilon), ...
                'Discount surplus does not match at oberservation %d',...
                data(row, logIndex)); 
            assert(le(abs(data(row, ccda1Index(entry)) - data(row, twomarketlaIndex(entry))), epsilon), ...
                'Discount surplus does not match at oberservation %d',...
                data(row, logIndex));  
        end       
        end
    catch err
        testResult=strcat(testResult, err.message, '\n');
    end 
end
    


