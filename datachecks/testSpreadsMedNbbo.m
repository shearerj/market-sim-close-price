function testResult=testSpreadsMedNbbo(filename)
% testResult=testSpreadsMedNbbo(filename)
%
% Testing whether spreads at med_nbbo value at latency=0 for each model 
%       in the same line is the same.
%
% Example: testZSpreadsMedNbbo('example.mat');
%          (where the data is saved in example.mat)

load(filename);
[datar, datac] = size(data);

testResult = '';
epsilon = 1e-3;

latencyIndex = getIndex(headers, 'latency');
assert(~isempty(latencyIndex), 'Latency column does not exist!');

medNbboIndex = getIndex(headers, 'med_nbbo');
[surpr, surpc] = size(medNbboIndex);

logIndex = getIndex(headers, 'obs');

groupNum = surpr/4;

for row=1:datar
    if (data(row, latencyIndex) == 0)
        try
        for i = 1:groupNum-1
            for j = 1:4
            assert(le(abs(data(row, medNbboIndex(i*4+j))- ...
                data(row, medNbboIndex(j))), epsilon), ...
                'Spreads at med nbbo for 0 latency does not match at oberservation %d', ...
                data(row, logIndex));
            end
        end
        catch err
            testResult=strcat(testResult, err.message, '\n');
        end
    end    
end
    


