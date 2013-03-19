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
result = testdiscSurplus('example.mat');
if(~strcmp(result, ''))
     fprintf(fileID, 'FAILED\n');
     fprintf(fileID, result);
else fprintf(fileID, 'PASSED\n');
end


fclose(fileID);