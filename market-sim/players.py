import numpy as np
import json
import os

n=100
holdings = np.empty(n)

for i in range(n):
	os.system('< resources/bench.json ./market-sim | jq -c \'.players\' > playersTest.json')
	file = open('playersTest.json')
	players = json.load(file)
	file.close()
	for j,play in enumerate(players):
		strat = play['strategy']
		if strat == 'b':
			holdings[i] =play['features']['holdings']

print(np.average(holdings))