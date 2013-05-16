function dataChecks(filename)
% Data checks script
%
% Authors: ewah, jiesongk
% Last modified: 2013-05-16

%[data headers] = readCSV(filename);

load(filename); % -------------------here filename is example.mat
[datar, ~] = size(data);

fileID = fopen([filename '_datachecks.log'],'w');


% set up constants
latencyIndex = getIndex(headers, 'latency');
assert(~isempty(latencyIndex), 'Latency column does not exist!');

surpSumTotalIndex = getIndex(headers, 'surplus_sum_total');
transactionNumIndex = getIndex(headers, 'transactions_num');
ccda1Index = getIndex(headers, 'centralcda1_surplus_disc');
twomarketdummyIndex = getIndex(headers, 'twomarketdummy_surplus_disc');
twomarketlaIndex = getIndex(headers, 'twomarketla_surplus_disc');
intervalSumIndex = getIndex(headers, 'interval_sum');
execSpeedIndex = getIndex(headers, 'exec_speed');
medNbboIndex = getIndex(headers, 'med_nbbo');
priceVolIndex = getIndex(headers, 'price_vol');

logIndex = getIndex(headers, 'obs');


eps = 1e-3;
intervalSumEpsilon = 15000; % ---------look into this


surplusResult = '';
transResult = '';
discSurplusResult = '';
intervalResult = '';
priceVolResult = '';
execSpeedResult = '';
nbboResult = '';

for row=1:datar
    % --------------------------------------------------------------------
    % test for matching surplus at zero latency
    try
        assert(data(row, latencyIndex)<= 1000 && data(row, latencyIndex)>= 0,...
            'Latency out of range at obs %d', ...
            data(row, logIndex));
    catch err
        surplusResult=strcat(surplusResult, err.message, '\n');
    end
    if (data(row, latencyIndex) == 0)
        surpSumTotal = data(row, surpSumTotalIndex);
        try
            for j=2:size(surpSumTotalIndex,1)
                assert(le(abs(surpSumTotal(1, j)- surpSumTotal(1, 1)), eps), ...
                    'Surplus sum total for latency 0 does not match at obs %d',...
                    data(row, logIndex));  
            end
        catch err
            surplusResult=strcat(surplusResult, err.message, '\n');
        end
    end
    
    % --------------------------------------------------------------------
    % test for transaction number for latency 0
    try
        if data(row, latencyIndex) == 0
            for j=2:size(transactionNumIndex,1)-1
                assert(le(abs(data(row, transactionNumIndex(j))...
                    - data(row, transactionNumIndex(1))), eps), ...
                    'Transaction number for latency 0 does not match at obs %d',...
                    data(row, logIndex));  
            end
        end
    catch err
        transResult=strcat(transResult, err.message, '\n');
    end
    
    % --------------------------------------------------------------------
    % test for interval sum (whether or not agents arrive before simulation
    % end)
    try
        assert(le(data(row, intervalSumIndex), intervalSumEpsilon), ...
            'Interval sum does not match at obs %d',...
            data(row, logIndex));  
    catch err
        intervalResult=strcat(intervalResult, err.message, '\n');
    end
    
    % --------------------------------------------------------------------
    % test for price vol at 0 latency
    try
        if data(row, latencyIndex) == 0
            for j=2:size(priceVolIndex,1)-1
                assert(le(abs(data(row, priceVolIndex(j)) - data(row, priceVolIndex(1))), eps), ...
                    'Price Vol for latency 0 does not match at obs %d',...
                    data(row, logIndex));  
            end
        end
    catch err
        priceVolResult=strcat(priceVolResult, err.message, '\n');
    end
    
    
    % --------------------------------------------------------------------
    % test for execution speed at 0 latency
    if (data(row, latencyIndex) == 0)
        try
        for i = 1:size(execSpeedIndex,1)/4-1 % ------------------------------should not be hard-coded
            for j = 1:4
            assert(le(abs(data(row, execSpeedIndex(i*4+j))- ...
                data(row, execSpeedIndex(j))), eps), ...
                'Execution Speed for 0 latency does not match at obs %d %d %d', ...
                data(row, logIndex),data(row, execSpeedIndex(i*4+j)),...
                data(row, execSpeedIndex(j)));
            end
        end
        catch err
            execSpeedResult=strcat(execSpeedResult, err.message, '\n');
        end
    end    

    % --------------------------------------------------------------------
    % test for spreads of med nbbo at 0 latency
    if (data(row, latencyIndex) == 0)
        try
            for i = 1:size(medNbboIndex,1)/4-1 % ------------------------------should not be hard-coded
                for j = 1:4
                assert(le(abs(data(row, medNbboIndex(i*4+j))- ...
                    data(row, medNbboIndex(j))), eps), ...
                    'Spreads at med nbbo for 0 latency does not match at obs %d', ...
                    data(row, logIndex));
                end
            end
        catch err
            nbboResult=strcat(nbboResult, err.message, '\n');
        end
    end  
end





% --------------------------------------------------------------------
% test for discount surplus for all models (not central call) at latency 0
try
    if data(row, latencyIndex) == 0
        for entry = 1:size(ccda1Index,1)
            assert(le(abs(data(row, ccda1Index(entry)) - ...
                data(row, twomarketdummyIndex(entry))), eps), ...
                'Discount surplus does not match at obs %d',...
                data(row, logIndex)); 
            assert(le(abs(data(row, ccda1Index(entry)) - ...
                data(row, twomarketlaIndex(entry))), eps), ...
                'Discount surplus does not match at obs %d',...
                data(row, logIndex));
        end
    end
catch err
    discSurplusResult=strcat(discSurplusResult, err.message, '\n');
end




% --------------------------------------------------------------------
% print out all results
fprintf(fileID, 'Testing total surplus at latency 0: ');
printResult(surplusResult, fileID);
fprintf(fileID, 'Testing transaction num at latency 0: ');
printResult(transResult, fileID);
fprintf(fileID, 'Testing discount surplus at latency 0 except central call: ');
printResult(discSurplusResult, fileID);
fprintf(fileID, 'Testing interval sum: ');
printResult(intervalResult, fileID);
fprintf(fileID, 'Testing execution speed at latency 0: ');
printResult(execSpeedResult, fileID);
fprintf(fileID, 'Testing spreads of med nbbo at latency 0: ');
printResult(nbboResult, fileID);
fprintf(fileID, 'Testing price vol at latency 0: ');
printResult(priceVolResult, fileID);



fclose(fileID);

end



function printResult(result, fileID) 
% prints whether pass or fail

if(~strcmp(result, ''))
    fprintf(fileID, 'FAILED\n');
    fprintf(fileID, result);
else
    fprintf(fileID, 'PASSED\n');
end

end
