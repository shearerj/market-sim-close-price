import numpy as np
import json

# Volume weighted average price as a benchmark
def vwap(file_name):

	# Open our JSON file and load it into python
	file = open (file_name)
	transactions = json.load(file)

	prices = np.empty(len(transactions))

	for i,t in enumerate(transactions):
		prices[i] = t[1]

	vwap = np.average(prices)
	return vwap

#  Volume weighted median price as a benchmark
def vwmp(file_name):

	# Open our JSON file and load it into python
	file = open (file_name)
	transactions = json.load(file)

	prices = np.empty(len(transactions))

	for i,t in enumerate(transactions):
		prices[i] = t[1]

	vwap = np.med(prices)
	return vwap


def main():
	file_name = 'prices.json'
	benchmark = vwap(file_name)
	print(benchmark)

if __name__ == "__main__":
    main()