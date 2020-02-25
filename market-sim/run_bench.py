import os
import json
import numpy as np
from benchmark import vwap

#os.system('make jar')

#seed = 500
benchmarkProp = 1

with open('resources/constFundpInfo.json', 'r') as f:
    json_data = json.load(f)
    json_data['configuration']['contractHoldings'] = "0"
    json_data['configuration']['benchmarkProp'] = str(benchmarkProp)

with open('resources/constFundpInfo.json', 'w') as f:
    f.write(json.dumps(json_data))

os.system(' < resources/constFundpInfo.json ./market-sim.sh | jq \'.players\' > players/players.json')
os.system(' < resources/constFundpInfo.json ./market-sim.sh | jq \'.features | .markets[0] | .prices\' > prices/prices.json')

file = open ('players/players.json')
players = json.load(file)

for i, play in enumerate(players):
	if play['strategy'] == 'b':
		no_mani_payoff = play['payoff']
		break

no_mani_benchmark = vwap('prices/prices.json')
print no_mani_payoff
print no_mani_benchmark

n = 10
mani_payoff = np.empty(n * 2 + 1)
mani_benchmark = np.empty(n * 2 + 1)

for i in range(-n,n + 1):
	k = i * 100
	with open('resources/constFundpInfo.json', 'r') as f:
	    json_data = json.load(f)
	    json_data['configuration']['contractHoldings'] = str(k)
	    json_data['configuration']['benchmarkProp'] = str(benchmarkProp)

	with open('resources/constFundpInfo.json', 'w') as f:
	    f.write(json.dumps(json_data))

	os.system(' < resources/constFundpInfo.json ./market-sim.sh | jq \'.players\' > players/players' + str(k) + '.json')
	os.system(' < resources/constFundpInfo.json ./market-sim.sh | jq \'.features | .markets[0] | .prices\' > prices/prices' + str(k) + '.json')

	file = open ('players/players' + str(k) + '.json')
	players = json.load(file)

	for j, play in enumerate(players):
		if play['strategy'] == 'b':
			mani_payoff[i + n] = play['payoff']
			break

	mani_benchmark[i + n] = vwap('prices/prices' + str(k) + '.json')


np.savetxt('mani_payoff.txt', mani_payoff, delimiter='\n') 
np.savetxt('mani_benchmark.txt', mani_benchmark, delimiter='\n') 
