import json
import numpy as np

def main():
    stats = {}

    for i in range(200):
    	entry = f'out-{i}'
    	stats[entry] = json.load(f'drl_env1/output-{i}.json')

    print(stats)


if __name__ == '__main__':
    main()