# experiments_latency.py
#
# This files run simulations while allowing the user to choose the variable that will be varied in the simulations. At the end it also
# runs various datachecks on the data to make sure they make sense.It also gives the user the option to choose the minimum and maximum
# values and the increment that the varaible will iterate over.

import sys, time, os, shutil

if not(len(sys.argv) == 6):
    print ("REMINDER: Compile jar with ant beforehand")
    print ("Usage: python experiments_variable.py [experiment name] [num samples] [test var] [set iteration range (yes or no)] [email address]")
    print ("Example: python experiments_variable.py MY_EXPERIMENT 100 latency no ewah@umich.edu")
    print ("Acceptable test variable: latency, sim_length, arrival_rate, tick_size, reentry_rate, mean_value, kappa, shock, private_value")
    quit()

#default parameters
latency = 0
sim_length = 15000
arrival_rate = 0.075
tick_size = 1
reentry_rate = 0.005
mean_value = 100000
kappa = 0.05
shock = 150000000
private_value = 100000000

output = '%s_output.log' %sys.argv[1]
csv = '%s.csv' %sys.argv[1]

f = open(output, 'w')
f.write('start time: %s \n' %time.asctime(time.localtime(time.time())))

if sys.argv[4] == "yes":
    min_range = input("Minimum range: ")
    max_range = input("Maximum range: ")
    incr = input("Increment: ")
elif sys.argv[3] == "latency":
    min_range = 0
    max_range = 1000
    incr = 100
elif sys.argv[3] == "sim_length":
    min_range = 10000
    max_range = 20000
    incr = 2000
elif sys.argv[3] == "arrival_rate":
    min_range = 0.025
    max_range = 0.2
    incr = 0.025
elif sys.argv[3] == "tick_size":
    min_range = 1
    max_range = 10
    incr = 1
elif sys.argv[3] == "reentry_rate":
    min_range = 0.005
    max_range = 0.01
    incr = 0.001
elif sys.argv[3] == "mean_value":
    min_range = 10000
    max_range = 100000
    incr = 10000
elif sys.argv[3] == "kappa":
    min_range = 0.01
    max_range = 0.1
    incr = 0.01
elif sys.argv[3] == "shock":
    min_range = 100000000
    max_range = 200000000
    incr = 50000000
elif sys.argv[3] == "private_value":
    min_range = 50000000
    max_range = 150000000
    incr = 10000000
else:
    print("Not a valid test parameter")
    sys.exit(1)

# test value while loop
i = min_range
while i <= max_range:
    if sys.argv[3] == "arrival_rate" or sys.argv[3] == "reentry_rate" or sys.argv[3] == "kappa":
        FOLDER = 'simulations/%s_%s_%.3f' %(sys.argv[1], sys.argv[3], i)
    else:
        FOLDER = 'simulations/%s_%s_%d' %(sys.argv[1], sys.argv[3], i)
    
    if sys.argv[3] == "latency":
        latency = i
    elif sys.argv[3] == "sim_length":
        sim_length = i
    elif sys.argv[3] == "arrival_rate":
        arrival_rate = i
    elif sys.argv[3] == "tick_size":
        tick_size = i
    elif sys.argv[3] == "reentry_rate":
        reentry_rate = i
    elif sys.argv[3] == "mean_value":
        mean_value = i
    elif sys.argv[3] == "kappa":
        kappa = i
    elif sys.argv[3] == "shock":
        shock = i
    elif sys.argv[3] == "private_value":
        private_value = i
    
    if not(os.path.isdir(FOLDER)):
        os.mkdir(FOLDER)
    else:
        shutil.rmtree('%s/*' %FOLDER, ignore_errors = True)
    #os.chmod('normal_create_spec_file.sh', 0700)
    os.system('./new_create_spec_file.sh %s/simulation_spec.json %d %d %.3f %d %.3f %d %.2f %d %d' %(FOLDER,latency,sim_length,arrival_rate,tick_size,reentry_rate,mean_value,kappa,shock,private_value))
    f.write('Samples for %s \n' %FOLDER)
    #os.chmod('run_hft.sh', 0700)
    os.system('./run_hft.sh %s %s >> %s' %(FOLDER, sys.argv[2], output))
    f.write("Parsing \n")
    #os.chmod('parse_single.sh', 0700)
    os.system('./parse_single.sh %s %s >> %s' %(csv, FOLDER, output))
    i += incr
#end of while loop

f.write('end time: %s \n \n' %time.asctime(time.localtime(time.time())))
os.system("matlab -nojvm -nodisplay -nosplash -r 'dataChecks2 %s %s; quit'" %(csv, sys.argv[1]))

f2 = open('%sDataCheckResult.txt' %sys.argv[1], "r")
for line in f2:
    f.write(line)
f.close()
f2.close()

os.system('mail -s "[HFT EXPERIMENT] %s " "%s" < %s' %(sys.argv[1], sys.argv[5], output))

os.remove(output)
os.remove('%sDataCheckResult.txt' %sys.argv[1])
os.remove('%s.mat' %sys.argv[1])


