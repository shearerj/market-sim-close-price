import json
import numpy as np
from functools import reduce

def main():
    stats = {}
    paths = {}

    for i in range(200):
    	entry = f'out-{i}'
    	with open(f'drl_env1/output-{i}.json') as json_file:
    		stats[entry] = json.load(json_file)
    		paths[entry] = ['Benchmark-Manipulator', 'mean']

	print(max(stats, key=lambda k: value(k, stats, paths[k])))


if __name__ == '__main__':
    main()