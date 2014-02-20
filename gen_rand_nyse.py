# to run:
#	python gen_rand_nyse.py [number_of_orders_to_print]



# NYSE supports the following orders
	# Add Order 
	# Modify Order 
	# Delete Order
	# Imbalance 
	# System Event

# examples:
	# A,1,12884902522,B,B,4900,AAIR,0.1046,28800,390,B,AARCA,
	# D,73,12884904105,30112,692,GRZG,B,B,AARCA,B,
	# M,85,12884903642,760,0.892,28800,531,GNIN,B,B,AARCA,B,

import random
import sys


numOrders = int(sys.argv[1]);

msg_type = ['A','M','D','I'];
seq_num = 0;
ord_ref_num = 0; #should have set of ord_ref_nums to pick from when generating output?
				# can be upped from 1 (not really important)
exchg_code = ['N','P','B'];
buy_or_sell = ['B','S'];
shares = 3000;
stock_sym = 'SRG';
price =1;
seconds=0;
milliseconds=0;
sys_code = ['L','O','E','B'];
quote_id =1; 
filler =1;
total_imbalance = 1;
market_imbalance = 1;
auction_type = ['O','M','H','C'];
auction_time = 1;

for x in range(0,numOrders):

	#generate random values for variables
	m = random.choice(msg_type);

	shares = random.randint(0,1000);
	stock_sym = 'SRG' + str(random.randint(0,9));
	price = random.randint(1,9999);

	#go without modify, make sequential data 
	if(m == 'A'):
		print m , seq_num , ord_ref_num , random.choice(exchg_code) , random.choice(buy_or_sell) , shares , stock_sym , price , seconds , milliseconds , random.choice(sys_code), quote_id , filler;

	if(m == 'M'):
		print m , seq_num , ord_ref_num ,  shares ,  price , seconds , milliseconds , stock_sym ,random.choice(exchg_code) , random.choice(sys_code), quote_id, random.choice(buy_or_sell) , filler;

	if(m == 'D'):
		print m , seq_num , ord_ref_num , random.choice(exchg_code) , seconds , milliseconds , stock_sym , random.choice(exchg_code) , random.choice(sys_code), quote_id , random.choice(buy_or_sell) , filler;
	
	if(m == 'I'):
		print m , seq_num , ord_ref_num , stock_sym , price, shares, total_imbalance, seconds , milliseconds ,  market_imbalance, random.choice(auction_type), auction_time, random.choice(exchg_code) , random.choice(sys_code),  filler;


	seq_num = (seq_num + 1) % 9999999999;

	milliseconds+=random.randint(0,999);
	if(milliseconds >= 1000):
	 	seconds+= milliseconds/1000;
	 	milliseconds = milliseconds % 1000;
	ord_ref_num+=1;