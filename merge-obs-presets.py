#! /usr/bin/env python
from sys import argv, stdout, exit
import json

"""
Merges observation files from different PRESET runs, although it will work for
any that have different names
"""

def printUsage():
    print "Merges observation files, and returns summary statistics"
    print 
    print "Usage:", argv[0], "obs-files > merged-obs-file"
    print
    print "       for OBS in {0..99}; do"
    print "          ", argv[0], "directory/{CALL,CDA,TWOMARKET}/observation$OBS.json > directory/merged/observation$OBS.json"
    print "       done"

def mergeObs(filenames):
    
    out = {}
    feats = {}
    out['features'] = feats

    for filename in filenames:
        with open(filenames[0], 'r') as first:
            obs = json.load(first)

        players = obs['players']
        features = obs['features']
        config = features['config']
        features.pop('config')
        name = config['modelName']

        for feat, val in features.iteritems():
            feats[name + '_' + feat] = val
        out[name + '_config'] = config
        out[name + '_players'] = players

    return out

if __name__ == "__main__":
    if len(argv) < 2 or argv[1] == "-h" or argv[1] == "--help":
        printUsage()
        exit(1)
    json.dump(mergeObs(argv[1:]), stdout)
    stdout.write('\n')
