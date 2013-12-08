#! /usr/bin/env python
from sys import argv, stdout, exit
import json

"""
Merges observation files into a csv
"""

def printUsage():
    print "Merges observation files into a csv"
    print 
    print "Usage:", argv[0], "obs-files > csv-file"
    print
    print "This merges observation files into a csv. It only reports the \"features\""

def to_csv(out, filenames):
    with open(filenames[0], 'r') as first:
        obs = json.load(first)
    keys = set(obs['features'].keys())
    keys.discard('config')
    
    out.write(','.join(keys))
    out.write('\n')

    for filename in filenames:
        with open(filenames[0], 'r') as f:
            obs = json.load(f)
        feats = obs['features']
        out.write(','.join(str(feats[k]) for k in keys))
        out.write('\n')

if __name__ == "__main__":
    if len(argv) < 2 or argv[1] == "-h" or argv[1] == "--help":
        printUsage()
        exit(1)
    to_csv(stdout, argv[1:])
