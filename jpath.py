#! /usr/bin/env python
import argparse
import json
import sys

parser = argparse.ArgumentParser(description='Select specific objects from a json file, e.g. "./jpath.py -i observation0.json features"')
parser.add_argument('fields', metavar='field', nargs='*',
                    help='A successive field to inspect in the json file, strings for objects or positive indices for arrays')
parser.add_argument('-i', '--input', metavar='filename', type=argparse.FileType('r'), default=sys.stdin, 
                    help='The json file to read from, defaults to stdin')

if __name__ == '__main__':
    args = parser.parse_args()

    j = json.load(args.input)
    args.input.close()

    for field in args.fields:
        if field.isdigit():
            field = int(field)
        j = j[field]
    json.dump(j, sys.stdout, indent=4)
    sys.stdout.write('\n')
