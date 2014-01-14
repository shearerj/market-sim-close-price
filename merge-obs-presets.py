#! /usr/bin/env python
import sys
import json
import argparse

parser = argparse.ArgumentParser(description='Merges observation files from different preset runs and returns summary statistics. This is meant to account for past behavior where several presets were run at the same time. You may want to call something like "for OBS in {0..99}; do ' + sys.argv[0] + ' directory/{CALL,CDA,TWOMARKET}/observation$OBS.json > directory/merged/observation$OBS.json; done"')
parser.add_argument('files', metavar='obs-file', nargs='+',
                    help='An observation file to merge')
parser.add_argument('-o', '--output', metavar='merged-obs-file', type=argparse.FileType('w'), default=sys.stdout,
                    help='The merged observation file, defaults to stdout')

def mergeObs(filenames):
    
    out = {}
    feats = {}
    out['features'] = feats

    for filename in filenames:
        with open(filename, 'r') as first:
            obs = json.load(first)

        players = obs['players']
        features = obs['features']
        config = features['config']
        features.pop('config')
        name = config['modelName']

        for feat, val in features.iteritems():
            feats[name + '_' + feat] = float(val)
        out[name + '_config'] = config
        out[name + '_players'] = players

    return out

if __name__ == "__main__":
    args = parser.parse_args()
    json.dump(mergeObs(args.files), args.output)
    args.output.write('\n')
    args.output.close()
