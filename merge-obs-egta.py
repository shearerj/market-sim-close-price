#! /usr/bin/env python
from sys import argv, stdout, exit
from math import sqrt
import json

"""
Merges observation files like egta
"""

def printUsage():
    print "Merges observation files, and returns summary statistics"
    print 
    print "Usage:", argv[0], "obs-files > merged-obs-file"
    print "      ", argv[0], "obs-dir/observation*.json > merged-obs-file"
    print 
    print "\"features\" will contain the mean of all of the features per observation\n\
run. This could be changed in the future to also include standard\n\
deviations. The \"config\" feature will be the config of the last observation,\n\
but these should all be idential. The \"players\" object will contain an object\n\
nested by role and then by strategy. The final object has three field. \"mean\"\n\
which is simply the mean of all agent payoffs for that role and\n\
strategy. \"true_sample_stddev\" is the sample standard deviation of every pay\n\
off for every agent in every simulation. \"egta_sample_stddev\" is the sample\n\
standard deviation of the mean of all agent payoffs of that role and strategy in\n\
every observation. That is, payoffs are aggregated to a mean value for each role\n\
and strategy for each observation, then this is the standard deviation of those\n\
means across observations."

class SumStat:
    def __init__(self):
        self.n, self.sum, self.sumsq = 0, 0, 0

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

    def __sq_err(self):
        return self.sumsq - self.mean() ** 2

    def sample_stddev(self):
        return sqrt(self.__sq_err() / (self.n - 1))

def mergeObs(filenames):
    
    true_player_stats = {}
    egta_player_stats = {}
    feature_stats = {}

    for filename in filenames:
        with open(filenames[0], 'r') as first:
            obs = json.load(first)

        players = obs['players']
        features = obs['features']
        config = features['config']
        features.pop('config')
        n = int(config['numSims'])

        for feat, val in features.iteritems():
            feature_stats.setdefault(feat, SumStat()).add_one(val)

        sim_egta_stats = {}

        for player in players:
            true_stat = true_player_stats.setdefault(player['role'], {}).setdefault(player['strategy'], SumStat())
            egta_stat = sim_egta_stats.setdefault(player['role'], {}).setdefault(player['strategy'], SumStat())
            mean = float(player['payoff'])
            true_stat.add_many(n, mean, float(player['features']['payoff_stddev']) ** 2 * (n - 1) + mean ** 2)
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
    if len(argv) < 2 or argv[1] == "-h" or argv[1] == "--help":
        printUsage()
        exit(1)
    json.dump(mergeObs(argv[1:]), stdout)
    stdout.write('\n')
