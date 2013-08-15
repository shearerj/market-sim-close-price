import sys
from sys import argv
import os.path
import re
from Queue import PriorityQueue

"""
Merges log files. This is very unsafe in terms of file handling, but if it's
only used as a one time script it should work fine.
"""

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

def merge(logs):
    "Merges log files off of time. Takes a generator of file descriptors"
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
            sys.stdout.write(line)
        #line = reader.nextline()
        if not line:
            continue
        #sys.stdout.write(line)
        queue.put(reader)

if __name__ == "__main__":
    if len(argv) < 2:
        print "Merges various log files for easy comparison"
        print ""
        print "Usage: python merge_logs.py [log files] > new_log_file"
        print "       python merge_logs.py <log_dir>/* > new_log_file"
        sys.exit(1)
    merge(open(f) for f in argv[1:])
    
