#!/usr/bin/env python
import argparse
import json
import sys
import os
from os import path
import itertools

from numpy import random

parser = argparse.ArgumentParser(description='Sample player strategies for simulation spec files from a distribution of strategies by role.')
parser.add_argument('-s', '--simspec', type=argparse.FileType('r'), required=True, help='A simulation spec file for each sample. Must also including the number of players per role in a root item called "role_counts".')
parser.add_argument('-f', '--profile', type=argparse.FileType('r'), default=sys.stdin, help='A mixed strategy profile indicating the sample probability for each strategy.')
parser.add_argument('-n', '--num-samples', type=int, default=1, help='The number of samples to incorporate into the average social welfare.')
parser.add_argument('-d', '--directory', help='The directory to put all of the simulations in.')

def sample_players(profile, players):
    assignment = {}
    for role, probs in profile.iteritems():
        strats, probs = zip(*probs.items())
        assignment[role] = list(itertools.chain.from_iterable(
            itertools.repeat(x, y) for x, y
            in zip(strats, random.multinomial(players[role], probs))))
    return assignment

if __name__ == '__main__':
    args = parser.parse_args()

    simspec = json.load(args.simspec)
    role_counts = simspec['role_counts']
    profile = json.load(args.profile)

    # Create directory structure
    fmt = "%0" + str(len(str(args.num_samples - 1))) + "d"
    for i in xrange(args.num_samples):
        sim_dir = path.join(args.directory, fmt % i)
        simspec['assignment'] = sample_players(profile, role_counts)
        os.mkdir(sim_dir)
        with open(path.join(sim_dir, 'simulation_spec.json'), 'w') as f:
            json.dump(simspec, f)
