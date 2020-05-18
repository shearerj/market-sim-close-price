import json
import numpy as np

def main():
    stats = {}

    for i in range(200):
    	entry = f'out-{i}'
    	with open(f'drl_env1/output-{i}.json') as json_file:
    		stats[entry] = json.load(json_file)

    print(stats)


if __name__ == '__main__':
    main()