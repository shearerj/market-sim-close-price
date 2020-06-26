import json
import numpy as np
from functools import reduce
import os.path
from os import path

def main():
    stats = {}
    paths = {}

    for i in range(1000):
        entry = f'out-{i}'
        fPath = f'drl_env3/output-{i}.json'
        if path.exists(fPath):
            with open(fPath) as json_file:
                stats[entry] = json.load(json_file)
                paths[entry] = ['Benchmark-Manipulator', 'mean']
    print(max(stats, key=lambda k: stats[k]['Benchmark-Manipulator']['mean']))


if __name__ == '__main__':
    main()

