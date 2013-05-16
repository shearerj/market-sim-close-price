fileID = fopen('dataCheckResult.txt','w');

% test for matching surplus at zero latency
fprintf(fileID, 'Testing total surplus at latency 0: ');

result = testZeroLaSurplus('example.mat');
if(~strcmp(result, ''))
     fprintf(fileID, 'FAILED\n');
     fprintf(fileID, result);
else fprintf(fileID, 'PASSED\n');
end

% test for transaction number for latency 0
fprintf(fileID, 'Testing transaction num at latency 0: ');
result = testLAtransaction('example.mat');
if(~strcmp(result, ''))
     fprintf(fileID, 'FAILED\n');
     fprintf(fileID, result);
else fprintf(fileID, 'PASSED\n');
end

% test for discount surplus for all models except central call at latency
% 0
fprintf(fileID, 'Testing discount surplus at latency 0 except central call: ');
result = testdiscSurplus('example.mat');
if(~strcmp(result, ''))
     fprintf(fileID, 'FAILED\n');
     fprintf(fileID, result);
else fprintf(fileID, 'PASSED\n');
end

% test for interval sum 
fprintf(fileID, 'Testing interval sum: ');
result = testIntervalSum('example.mat');
if(~strcmp(result, ''))
     fprintf(fileID, 'FAILED\n');
     fprintf(fileID, result);
else fprintf(fileID, 'PASSED\n');
end

% test for execution speed at 0 latency
fprintf(fileID, 'Testing execution speed at latency 0: ');
result = testZeroLaExecSpeed('example.mat');
if(~strcmp(result, ''))
     fprintf(fileID, 'FAILED\n');
     fprintf(fileID, result);
else fprintf(fileID, 'PASSED\n');
end

% test for spreads of med nbbo at 0 latency
fprintf(fileID, 'Testing spreads of med nbbo at latency 0: ');
result = testSpreadsMedNbbo('example.mat');
if(~strcmp(result, ''))
     fprintf(fileID, 'FAILED\n');
     fprintf(fileID, result);
else fprintf(fileID, 'PASSED\n');
end

% test for price vol at 0 latency
fprintf(fileID, 'Testing price vol at latency 0: ');
result = testZeroLaPriceVol('example.mat');
if(~strcmp(result, ''))
     fprintf(fileID, 'FAILED\n');
     fprintf(fileID, result);
else fprintf(fileID, 'PASSED\n');
end


fclose(fileID);