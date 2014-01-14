#! /usr/bin/env python
import sys
import json
import argparse

parser = argparse.ArgumentParser(description='Merge several observation files into a csv. It only reports the "features" and forgets about player information.')
parser.add_argument('files', metavar='obs-file', nargs='+',
                    help='An observation file to include in the final csv')
parser.add_argument('-o', '--output', metavar='csv-file', type=argparse.FileType('w'), default=sys.stdout,
                    help='The csv file to write to, defaults to stdout')

def to_csv(out, filenames):
    with open(filenames[0], 'r') as first:
        obs = json.load(first)
    keys = set(obs['features'].keys())
    keys.discard('config')
    
    out.write(','.join(keys))
    out.write('\n')

    for filename in filenames:
        with open(filename, 'r') as f:
            obs = json.load(f)
        feats = obs['features']
        out.write(','.join(str(feats[k]) for k in keys))
        out.write('\n')

if __name__ == "__main__":
    args = parser.parse_args()
    to_csv(args.output, args.files)
