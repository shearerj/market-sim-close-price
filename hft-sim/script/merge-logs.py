#! /usr/bin/env python
import sys
import re
import argparse
from os import path
from Queue import PriorityQueue

parser = argparse.ArgumentParser(description='''Merges log files for easy comparison. This is very unsafe in terms of file handling (tries to open a lot of files, and will crash if want to merge more then the file system will allow), but if it's only used as a one time script it should work fine.''')
parser.add_argument('files', metavar='log-file', nargs='+', type=argparse.FileType('r'),
                    help='A log file to merge')
parser.add_argument('-o', '--output', '-f', '--file', metavar='merged-log-file', type=argparse.FileType('w'), default=sys.stdout,
                    help='The log file to write to, defaults to stdout')

# Regex for time of a line
retime = re.compile(r'\s*(\d+)')

class LogReader:
    def __init__(self, f):
        self.f = f
        self.line = f.readline()
        self.time = 0
        self.sim = 0

    def nextline(self):
        line = self.line
        self.line = self.f.readline()
        match = retime.match(self.line)
        if match:
            time = int(match.group(1))
            if time < self.time:
                self.sim += 1
            self.time = time
        else:
            self.time = sys.maxint
        return line

    def __lt__(self, other):
        return (self.sim, self.time) < (other.sim, other.time)

    def __eq__(self, other):
        return (self.sim, self.time) == (other.sim, other.time)

def merge(logs, output):
    """ Merges log files off of time. Takes a generator of file descriptors """
    queue = PriorityQueue()

    paths = [path.dirname(path.abspath(f.name)) for f in logs]
    prefixLength = len(path.dirname(path.commonprefix(paths))) + 1
    names = [p[prefixLength:-5] for p in paths]
    length = max(len(n) for n in names)

    for i, (log, name) in enumerate(zip(logs, names)):
        queue.put((LogReader(log), name))

    while not queue.empty():
        reader, name = queue.get()
        line = reader.nextline()
        if not line:
            continue
        output.writelines((name.rjust(length), '|', line))
        queue.put((reader, name))

if __name__ == "__main__":
    args = parser.parse_args()
    merge(args.files, args.output)
    for file in args.files:
        file.close()
    args.output.close()
