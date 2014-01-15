#! /usr/bin/env python
import sys
import math
import json
import argparse
import textwrap

parser = argparse.ArgumentParser(description='\n'.join(textwrap.wrap('Merge successive observation files like egta. "features" will contain the mean of all of the features per observation run. This could be changed in the future to also include standard deviations. The "config" feature will be the config of the last observation, but these should all be idential. The "players" object will contain an object nested by role and then by strategy. The final object has three field. "mean" which is simply the mean of all agent payoffs for that role and strategy. "true_sample_stddev" is the sample standard deviation of every pay off for every agent in every simulation. "egta_sample_stddev" is the sample standard deviation of the mean of all agent payoffs of that role and strategy in every observation. That is, payoffs are aggregated to a mean value for each role and strategy for each observation, then this is the standard deviation of those means across observations.', width=78)),
                                 epilog='''example usage:
  ''' + sys.argv[0] + ''' simulation_directory/observation*.json > simulation_directory/merged_observation.json
  ''' + sys.argv[0] + ''' simulation_directory/observation*.json -o simulation_directory/merged_observation.json''',
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
parser.add_argument('files', metavar='obs-file', nargs='+',
                    help='An observation file to merge')
parser.add_argument('-o', '--output', '-f', '--file', metavar='merged-obs-file', type=argparse.FileType('w'), default=sys.stdout,
                    help='The merged json file to write to, defaults to stdout')

class SumStat:
    def __init__(self):
        self.n = self.sum = self.sumsq = 0.0

    def add_one(self, val):
        self.n += 1
        self.sum += val
        self.sumsq += val ** 2

    def add_many(self, n, sum, sumsq):
        self.n += n
        self.sum += sum
        self.sumsq += sumsq

    def mean(self):
        return float(self.sum) / self.n;

    def __sq_err__(self):
        return self.sumsq - self.sum ** 2 / self.n

    def sample_stddev(self):
        return math.sqrt(self.__sq_err__() / (self.n - 1))

def mergeObs(filenames):
    
    true_player_stats = {}
    egta_player_stats = {}
    feature_stats = {}

    for filename in filenames:
        with open(filename, 'r') as first:
            obs = json.load(first)

        players = obs['players']
        features = obs['features']
        config = features['config']
        features.pop('config')
        n = int(config['numSims'])

        for feat, val in features.iteritems():
            feature_stats.setdefault(feat, SumStat()).add_one(float(val))

        sim_egta_stats = {}

        for player in players:
            true_stat = true_player_stats.setdefault(player['role'], {}).setdefault(player['strategy'], SumStat())
            egta_stat = sim_egta_stats.setdefault(player['role'], {}).setdefault(player['strategy'], SumStat())
            mean = float(player['payoff'])
            true_stat.add_many(n, mean, float(player['features']['payoff_stddev']) ** 2 * (n - 1) + n * mean ** 2)
            egta_stat.add_one(mean)

        for role, rest in sim_egta_stats.iteritems():
            for strat, stat in rest.iteritems():
                egta_player_stats.setdefault(role, {}).setdefault(strat, SumStat()).add_one(stat.mean())
                
    players = {}
    for role, rest in true_player_stats.iteritems():
        for strat, stat in rest.iteritems():
            players.setdefault(role, {})[strat] = {
                'mean': stat.mean(),
                'true_sample_stddev': stat.sample_stddev(),
                'egta_sample_stddev': egta_player_stats[role][strat].sample_stddev()
                }

    features = { 'config': config }
    for feat, stat in feature_stats.iteritems():
        features[feat] = stat.mean()

    return { 'players': players, 'features': features }

if __name__ == "__main__":
    args = parser.parse_args()
    json.dump(mergeObs(args.files), args.output)
    args.output.write('\n')
    args.output.close()
