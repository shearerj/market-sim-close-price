function dataChecks(file)
% dataCheck(file)
%
% This function takes a csv file, runs data checks on it and returns a results file.
%
% Example: dataCheck('test.csv')
% Result: testResult.txt

% creates .mat file with data and headers
[data,headers] = readCSV(file);
string = file(1:end-4);
filename = [string, '.mat'];
save(filename, 'data', 'headers');

fname = 'dataCheckResult.txt';

load(filename);
fileID = fopen(fname,'w');

[datar, datac] = size(data);
latencyIndex = getIndex(headers, 'latency');
logIndex = getIndex(headers, 'obs');
epsilon = 1e-3;

%Header variables
noDiscSurpHeader = 'surplus_sum_total_nodisc';
transNumHeader = 'trans_zi_num';
discSurpHeader = 'surplus_sum_total_disc';
execSpeedHeader = 'exectime';
spreadMedNbboHeader = 'spreads_med_nbbo';
volMeanStdPriceHeader = 'mean_stdprice';
volStdPriceHeader = 'std_price_mkt';
volMeanLogPriceHeader = 'mean_logprice';
volStdLogReturnHeader = 'std_logreturn_mkt';
volMeanLogReturnHeader = 'mean_logreturn';

% Check for latency column
assert(~isempty(latencyIndex), 'Latency column does not exist!');

% test range of latency values
fprintf(fileID, 'Testing range of latency values: ');
test = true;
wrong = 0;
for row = 1:datar
   if data(row, latencyIndex) < 0 || data(row, latencyIndex) > 1000
      test = false;
      wrong = wrong + 1;
      if wrong == 1
         fprintf(fileID, 'FAILED\n');
         fprintf(fileID, '\t Latency out of range at Oberservation %d \n', data(row, logIndex));
      elseif wrong > 1
         fprintf(fileID, '\t Latency out of range at Oberservation %d \n', data(row, logIndex));
      end
   end
end

if test
   fprintf(fileID, 'PASSED\n');
end

% test for matching surplus at zero latency
noDiscSurpIndex = getIndex(headers, noDiscSurpHeader); 
fprintf(fileID, 'Testing total undiscounted surplus at latency 0: ');
testSome(noDiscSurpIndex, 'Undiscounted Surplus sum total for 0 latency does not match at oberservation');

% test for transaction number for latency 0
transNumIndex = getIndex(headers, transNumHeader); 
fprintf(fileID, 'Testing transaction num at latency 0: ');
testSome(transNumIndex, 'Transaction number for latency 0 does not match at oberservation');

% test for discount surplus for all models except central call at latency 0
discSurpIndex = getIndex(headers, discSurpHeader);
noCallDiscSurpIndex = discSurpIndex(2:end,1);
fprintf(fileID, 'Testing total discounted surplus at latency 0 except central call: ');
testSome(noCallDiscSurpIndex, 'Discount surplus does not match at oberservation');

% test for execution speed at 0 latency
execTimeIndex = getIndex(headers, execSpeedHeader); 
fprintf(fileID, 'Testing execution speed at latency 0: ');
testSome(execTimeIndex, 'Execution Speed for 0 latency does not match at oberservation');

% test for spreads of med nbbo at 0 latency
medNbboIndex = getIndex(headers, spreadMedNbboHeader); 
fprintf(fileID, 'Testing spreads of med nbbo at latency 0: ');
testSome(medNbboIndex, 'Spreads at med nbbo for 0 latency does not match at oberservation');

% test for avg vol (std dev of log of midquote prices) at 0 latency
volMeanStdPriceIndex = getIndex(headers, volMeanStdPriceHeader);
fprintf(fileID, 'Testing avg vol (std dev of log of midquote prices) at latency 0: ');
testVol(volMeanStdPriceIndex, 'Price vol (std dev of log of midquote prices) for latency 0 does not match at observation');

% test for avg vol (log of std dev of midquote prices) at 0 latency
volMeanLogPriceIndex = getIndex(headers, volMeanLogPriceHeader);
fprintf(fileID, 'Testing avg vol (log of std dev of midquote prices) at latency 0: ');
testVol(volMeanLogPriceIndex, 'Price vol (log of std dev of midquote prices) for latency 0 does not match at observation');

% test for avg vol (std dev of log returns) at 0 latency
volMeanLogReturnIndex = getIndex(headers, volMeanLogReturnHeader);
fprintf(fileID, 'Testing avg vol (std dev of log returns) at latency 0: ');
testVol(volMeanLogReturnIndex, 'Price vol (std dev of log returns) for latency 0 does not match at observation');

% test vol in markets (log of std dev of midquote prices) at 0 latency
volStdPriceIndex = getIndex(headers, volStdPriceHeader);
fprintf(fileID, 'Testing vol in markets (log of std dev of midquote prices) at latency 0: ');
testVol2(volStdPriceIndex, 'Price vol (log of std dev of midquote prices) for latency 0 does not match at observation', 1, 2, 3, 5, 4, 6);

% test vol in markets (std dev of log returns) at 0 latency
volStdLogReturnIndex = getIndex(headers, volStdLogReturnHeader);
fprintf(fileID, 'Testing vol in markets (std dev of log returns) at latency 0: ');
testVol2(volStdLogReturnIndex, 'Price vol (std dev of log returns) for latency 0 does not match at observation', 1, 2, 3, 6, 4, 5);

type(fname);

% Function that iterates through one index and checks for equality
   function testSome(dataIndex, errMessage)
      test = true;
      wrong = 0;
      [r, c] = size(dataIndex);
      for row = 1: datar
         if (data(row,latencyIndex) == 0)
            index = data(row, dataIndex);
            for j = 2:r - 2
               if ~(le(abs(index(1, j)- index(1, 1)),epsilon))
                  test = false;
                  wrong = wrong + 1;
                  if wrong == 1
                     fprintf(fileID, 'FAILED\n');
                     fprintf(fileID, '\t %s %d \n', errMessage, data(row, logIndex));
                  elseif wrong > 1
                     fprintf(fileID, '\t %s %d \n', errMessage, data(row, logIndex));
                  end
               end
            end
         end
      end

      if test
         fprintf(fileID, 'PASSED\n');
      end
   end

% Volatility function 1
   function testVol(dataIndex, errMessage)
      test = true;
      wrong = 0;
      for row = 1: datar
         if (data(row, latencyIndex) == 0)
            if (~(le(abs(data(row, dataIndex(1))- data(row, dataIndex(2))),epsilon))) || ...
               (~(le(abs(data(row, dataIndex(3))- data(row, dataIndex(4))),epsilon)))
               test = false;
               wrong = wrong + 1;
               if wrong == 1
                  fprintf(fileID, 'FAILED\n');
                  fprintf(fileID, '\t %s %d \n', errMessage, data(row, logIndex));
               elseif wrong > 1
                  fprintf(fileID, '\t %s %d \n', errMessage, data(row, logIndex));
               end
            end
         end
      end

      if test
         fprintf(fileID, 'PASSED\n');
      end
   end

% Volatility function 2
   function testVol2(dataIndex, errMessage, a, b, c, d, e, f)
      test = true;
      wrong = 0;
      for row = 1: datar
         if (data(row, latencyIndex) == 0)
            if (~(le(abs(data(row, dataIndex(a))- data(row, dataIndex(b))),epsilon))) || (~(le(abs(data(row, dataIndex(c))- ...
               data(row, dataIndex(d))),epsilon))) || (~(le(abs(data(row, dataIndex(e))- data(row, dataIndex(f))),epsilon)))
               test = false;
               wrong = wrong + 1;
               if wrong == 1
                  fprintf(fileID, 'FAILED\n');
                  fprintf(fileID, '\t %s %d \n', errMessage, data(row, logIndex));
               elseif wrong > 1
                  fprintf(fileID, '\t %s %d \n', errMessage, data(row, logIndex));
               end
            end
         end
      end

      if test
         fprintf(fileID, 'PASSED\n');
      end
   end   

end
