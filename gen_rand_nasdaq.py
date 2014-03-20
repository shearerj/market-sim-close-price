# to run:
#	python gen_rand_nasdaq.py [number_of_orders_to_print]



# NASDAQ supports the following orders
	# Add Order 
	# Modify Order 
	# Delete Order
	# Imbalance 
	# System Event


import random
import sys


numOrders = int(sys.argv[1]);

msg_type = ['A','F','E','C','X','D','U'];
Timestamp_nanoseconds = 0;
ord_ref_num = 0; 				# can be upped from 1 (not really important)
buy_or_sell = ['B','S'];
shares = 3000;
stock_sym = 'SRG';
price =1;
seconds=0;
milliseconds=0;
#sys_code = ['L','O','E','B'];
#quote_id =1; 
#total_imbalance = 1;
#market_imbalance = 1;
#auction_type = ['O','M','H','C'];
#auction_time = 1;
printable = ['N', 'Y'];

for x in range(0,numOrders):

	#generate random values for variables
	m = random.choice(msg_type);
	pick_printable = random.choice(printable);

	shares = random.randint(0,1000);
	stock_sym = 'SRGSRGS' + str(random.randint(0,9)); #needs to be 8 bytes
	price = random.randint(1,9999);

	#go without modify, make sequential data 
	if(m == 'A'):
		print m , Timestamp_nanoseconds , ord_ref_num , random.choice(buy_or_sell) , shares , stock_sym , price; 

	if(m == 'F'):
		print m , Timestamp_nanoseconds , ord_ref_num , random.choice(buy_or_sell) , shares , stock_sym , price; #needs a market identifier

	if(m == 'E'):
		print m , Timestamp_nanoseconds , ord_ref_num , shares #needs match number

	if(m == 'C'):
		print m , Timestamp_nanoseconds , ord_ref_num , shares , pick_printable , price; 

	if(m == 'X'):
		print m , Timestamp_nanoseconds , ord_ref_num , shares; 

	if(m == 'D'):
		print m , Timestamp_nanoseconds , ord_ref_num; 

	if(m == 'U'):
		print m , Timestamp_nanoseconds , ord_ref_num ,  shares ,  price; #new order reference number



	Timestamp_nanoseconds = (Timestamp_nanoseconds + 1);

	milliseconds+=random.randint(0,999);
	if(milliseconds >= 1000):
	 	seconds+= milliseconds/1000;
	 	milliseconds = milliseconds % 1000;
	ord_ref_num+=1;