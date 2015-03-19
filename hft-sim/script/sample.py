#!/usr/bin/env python
'''Python script to sample simulation spec files from a given role symmetric
mixed profile and role counts

'''
import argparse
import json
import sys
import os
from os import path
import itertools

from numpy import random

PARSER = argparse.ArgumentParser(description='''Sample player strategies for
simulation spec files from a distribution of strategies by role.''')
PARSER.add_argument('-s', '--simspec', type=argparse.FileType('r'),
                    required=True,
                    help='''A simulation spec file for each sample. Must also
                    including the number of players per role in a root item
                    called "role_counts". role_counts contains a mapping from
                    roles to the number of players for that role.''')
PARSER.add_argument('-f', '--profile', type=argparse.FileType('r'),
                    default=sys.stdin, required=True,
                    help='''A mixed strategy profile indicating the sample
                    probability for each strategy.''')
PARSER.add_argument('-n', '--num-samples', type=int, default=1,
                    help='''The number of samples to incorporate into the
                    average social welfare.''')
PARSER.add_argument('-d', '--directory', required=True,
                    help='''The directory to put all of the simulation spec
                    files in. Structure is
                    directory/<sample num>/simulation_spec.json''')

def sample_players(profile, players):
    '''Samples strategy counts from profile and counts'''
    assignment = {}
    for role, probs in profile.iteritems():
        # pylint: disable=no-member
        strats, probs = zip(*probs.items())
        assignment[role] = list(itertools.chain.from_iterable(
            itertools.repeat(x, y) for x, y
            in zip(strats, random.multinomial(players[role], probs))))
    return assignment


def main():
    '''Main script execution scoped to a function'''
    args = PARSER.parse_args()

    simspec = json.load(args.simspec)
    role_counts = simspec['role_counts']
    profile = json.load(args.profile)

    # Create directory structure
    fmt = "%0" + str(len(str(args.num_samples - 1))) + "d"
    for i in xrange(args.num_samples):
        sim_dir = path.join(args.directory, fmt % i)
        simspec['assignment'] = sample_players(profile, role_counts)
        os.mkdir(sim_dir)
        with open(path.join(sim_dir, 'simulation_spec.json'), 'w') as fil:
            json.dump(simspec, fil)

if __name__ == '__main__':
    main()
