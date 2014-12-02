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

def __read_field__(field):
    return int(field) if field.isdigit() else field


def get(full, jpath, value=None):
    j = full

    for field in jpath[:-1]:
        j = j[__read_field__(field)]

    if value is not None:
        assert jpath, "Fields must be none empty, otherwise you'd just be rewriting the file"
        j[__read_field__(jpath[-1])] = json.loads(value)
    elif jpath:
        j = j[__read_field__(jpath[-1])]
        full = j
        
    return full


if __name__ == '__main__':
    args = parser.parse_args()

    full = json.load(args.input)
    full = get(full, args.fields, args.value)

    json.dump(full, sys.stdout, indent=4, sort_keys=True)
    sys.stdout.write('\n')
