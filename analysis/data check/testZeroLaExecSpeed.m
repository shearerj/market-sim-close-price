function testResult=testZeroLaExecSpeed(filename)
% testResult=testZeroLaExecSpeed(filename)
%
% Testing whether exec_speed value at latency=0 for each model in the same
%       line is the same.
%
% Example: testZeroLaExecSpeed('example.mat');
%          (where the data is saved in example.mat)

load(filename);
[datar, datac] = size(data);

testResult = '';
epsilon = 1e-3;

latencyIndex = getIndex(headers, 'latency');
assert(~isempty(latencyIndex), 'Latency column does not exist!');

execSpeedIndex = getIndex(headers, 'exec_speed');
[surpr, surpc] = size(execSpeedIndex);

logIndex = getIndex(headers, 'obs');

groupNum = surpr/4;

for row=1:datar
    if (data(row, latencyIndex) == 0)
        try
        for i = 1:groupNum-1
            for j = 1:4
            assert(le(abs(data(row, execSpeedIndex(i*4+j))- ...
                data(row, execSpeedIndex(j))), epsilon), ...
                'Execution Speed for 0 latency does not match at oberservation %d %d %d', ...
                data(row, logIndex),data(row, execSpeedIndex(i*4+j)),...
                data(row, execSpeedIndex(j)));
            end
        end
        catch err
            testResult=strcat(testResult, err.message, '\n');
        end
    end    
end
    


