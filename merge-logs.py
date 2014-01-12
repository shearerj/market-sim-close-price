#! /usr/bin/env python
import sys
import os.path
import re
import argparse
from Queue import PriorityQueue

parser = argparse.ArgumentParser(description='Merges log files for easy comparison. This is very unsafe in terms of file handling (tries to open a lot of files, and will crash if want to merge more then the file system will allow), but if it\'s only used as a one time script it should work fine.')
parser.add_argument('files', metavar='log-file', nargs='+', type=argparse.FileType('r'),
                    help='A log file to merge')
parser.add_argument('-o', '--output', metavar='merged-log-file', type=argparse.FileType('w'), default=sys.stdout,
                    help='The log file to write to, defaults to stdout')

# Regex for time of a line
retime = re.compile(r'\d+\|\s*(\d+)')

class LogReader:
    def __init__(self, f):
        self.file = f
        self.line = f.readline()
        self.time = -1

    def nextline(self):
        line = self.line
        self.line = self.file.readline()
        match = retime.match(self.line)
        if match:
            self.time = int(match.group(1))
        elif self.time >= 0:
            self.time = sys.maxint
        return line

    def __lt__(self, other):
        return self.time < other.time

    def __eq__(self, other):
        return self.time == other.time

def merge(logs, output):
    """ Merges log files off of time. Takes a generator of file descriptors """
    queue = PriorityQueue()
    for log in logs:
        queue.put(LogReader(log))

    while not queue.empty():
        reader = queue.get()
        time = reader.time
        while reader.time == time:
            line = reader.nextline()
            if not line:
                break
            output.write(line)
        if not line:
            continue
        queue.put(reader)

if __name__ == "__main__":
    args = parser.parse_args()
    merge(args.files, args.output)
    for file in args.files:
        file.close()
    args.output.close()
