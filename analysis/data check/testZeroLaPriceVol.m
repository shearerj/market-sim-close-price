function testResult=testZeroLaPriceVol(filename)
% testResult=testZeroLaPriceVol(filename)
%
% Testing whether price vol value at latency=0 for each model 
%       in the same line is the same.
%
% Example: testZeroLaPriceVol('example.mat');
%          (where the data is saved in example.mat)

load(filename);
[datar, datac] = size(data);

testResult = '';
epsilon = 1e-3;

latencyIndex = getIndex(headers, 'latency');

priceVolIndex = getIndex(headers, 'price_vol')

[pvr, pvc] = size(priceVolIndex);

logIndex = getIndex(headers, 'obs');

for row=1:datar
    try
        if data(row, latencyIndex) == 0
        for j=2:pvr-1
            assert(le(abs(data(row, priceVolIndex(j)) - data(row, priceVolIndex(1))), epsilon), ...
                'Price Vol for latency 0 does not match at oberservation %d',...
                data(row, logIndex));  
        end
        end
    catch err
        testResult=strcat(testResult, err.message, '\n');
    end   
end
    

