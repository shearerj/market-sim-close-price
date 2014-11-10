#! /usr/bin/env python
import sys
import json
import argparse
import textwrap

parser = argparse.ArgumentParser(description='\n'.join(textwrap.wrap('Takes several observations with arbitrary numbers of players and strategies and returns a json file with the mean payoff for each role strategy pair. Each roll and strategy has a length two list of floats. The first float is the average payoff for every player who ever played that role strategy combination. The second number first averages over each role at the simulation level, and then averages the mean payoff for simulation.', width=78)),
                                 epilog='''example usage:
  ''' + sys.argv[0] + ''' simulation_directory/observation*.json > simulation_directory/merged_observation.json
  ''' + sys.argv[0] + ''' simulation_directory/observation*.json -o simulation_directory/merged_observation.json''',
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
parser.add_argument('files', metavar='obs-file', nargs='+', type=argparse.FileType('r'),
                    help='An observation file to merge')
parser.add_argument('-o', '--output', '-f', '--file', metavar='merged-obs-file', type=argparse.FileType('w'), default=sys.stdout,
                    help='The merged json file to write to, defaults to stdout')


class means(object):
    def __init__(self):
        self.n = 0
        self.mean = 0

    def add(self, val):
        self.n += 1
        self.mean += (val - self.mean) / self.n

    def __repr__(self):
        return str(self.mean)

def mergeObs(filelikes):    
    payoffs = {}

    for file in filelikes:
        obs = json.load(file)

        players = obs['players']
            
        sim_payoffs = {}

        for player in players:
            p = float(player['payoff'])
            payoffs.setdefault(player['role'], {}).setdefault(player['strategy'], (means(), means()))[0].add(p)
            sim_payoffs.setdefault(player['role'], {}).setdefault(player['strategy'], means()).add(p)

        for role, rest in sim_payoffs.iteritems():
            for strat, mean in rest.iteritems():
                payoffs[role][strat][1].add(mean.mean)
              
    # Ugly and hacked
    ret = {}
    for role, rest in payoffs.iteritems():
        for strat, res in rest.iteritems():
            ret.setdefault(role, {}).setdefault(strat, (res[0].mean, (res[1].mean * res[1].n) / len(filelikes)))

    return ret


if __name__ == "__main__":
    args = parser.parse_args()
    json.dump(mergeObs(args.files), args.output)
    args.output.write('\n')
    args.output.close()
