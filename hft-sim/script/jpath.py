#! /usr/bin/env python
import argparse
import json
import sys
import textwrap

parser = argparse.ArgumentParser(description='\n'.join(textwrap.wrap('Select specific fields and indicies from a json file, and print with formatting.', width=78)),
                                 epilog='''example usage:
  ''' + sys.argv[0] + ''' -i observation0.json features''',
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
parser.add_argument('fields', metavar='field', nargs='*',
                    help='A successive field to inspect in the json file, strings for objects or positive indices for arrays')
parser.add_argument('-i', '--input', '-f', '--file', metavar='filename', type=argparse.FileType('r'), default=sys.stdin, 
                    help='The json file to read from, defaults to stdin')
parser.add_argument('-v', '--value', metavar='value', 
                    help='A value to set the field to')

def read_field(field):
    return int(field) if field.isdigit() else field

if __name__ == '__main__':
    args = parser.parse_args()

    full = json.load(args.input)
    args.input.close()
    j = full

    for field in args.fields[:-1]:
        j = j[read_field(field)]

    if args.value is not None:
        assert args.fields, "Fields must be none empty, otherwise you'd just be rewriting the file"
        j[read_field(args.fields[-1])] = json.loads(args.value)
        j = full
    elif args.fields:
        j = j[read_field(args.fields[-1])]

    json.dump(j, sys.stdout, indent=4, sort_keys=True)
    sys.stdout.write('\n')
