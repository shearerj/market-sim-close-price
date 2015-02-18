#!/usr/bin/env python
import argparse
import json
import sys
import os
from os import path
import itertools
import tempfile
import shutil
import subprocess

from numpy import random

parser = argparse.ArgumentParser(description='Calculate the average social welfare given a set of observation files.')
parser.add_argument('observation', nargs='+', help='The observation files to calculate social welfare for and average.')

if __name__ == '__main__':
    args = parser.parse_args()

    welfare = 0.0        
    for obs in args.observation:
        with open(obs) as f:
            results = json.load(f)['players']
        welfare += sum(float(x['payoff']) for x in results)
        
    print welfare / len(args.observation)
