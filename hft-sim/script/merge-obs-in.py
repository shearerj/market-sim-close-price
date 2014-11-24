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

    def addn(self, val, n):
        self.n += n
        self.mean += (val - self.mean) * n / self.n

    def __repr__(self):
        return str(self.mean)


class ObsEncoder(json.JSONEncoder):
    def default(self, obj):
        if isinstance(obj, means):
            return obj.mean
        elif isinstance(obj, set):
            if len(obj) == 1:
                return next(iter(obj))
            else:
                return list(obj)
        else:
            return json.JSONEncoder.default(self, obj)


def mergeObs(filelikes):    
    payoffs = {}
    payoffs_sim = {}
    configuration = {}
    features = {}

    for f in filelikes:
        obs = json.load(f)

        # Player Processing
        players = obs.get('players', [])
        sim_payoffs = {}
        for player in players:
            # Add info
            info = payoffs.setdefault(player['role'], {}).setdefault(player['strategy'], {
                'payoff': means(),
                'features': {}
            })
            sinfo = sim_payoffs.setdefault(player['role'], {})\
                               .setdefault(player['strategy'], {
                                   'payoff': means(),
                                   'features': {}
                               })
            # Update payoff means
            p = float(player['payoff'])
            info['payoff'].add(p)
            sinfo['payoff'].add(p)
            # Update features
            feats = info['features']
            sfeats = sinfo['features']
            for feat, val in player.get('features', {}).iteritems():
                feats.setdefault(feat, means()).add(val)
                sfeats.setdefault(feat, means()).add(val)
            
        for role, rest in sim_payoffs.iteritems():
            for strat, avg in rest.iteritems():
                info = payoffs_sim.setdefault(role, {}).setdefault(strat, {
                    'payoff': means(),
                    'features': {}
                })
                info['payoff'].add(avg['payoff'].mean)
                feats = info['features']
                for feat, val in avg['features'].iteritems():
                    feats.setdefault(feat, means()).add(val.mean)

        feats = obs.get('features', {})
        # Configuration Processing        
        config = feats.pop('config', {})
        for param, setting in config.iteritems():
            configuration.setdefault(param, set()).add(setting)

        # Feature processing
        for feat, val in feats.iteritems():
            features.setdefault(feat, means()).add(val)

    # This adds a zero entry for every payoff that had no values during a
    # single run
    numf = len(filelikes)
    for role, rest in payoffs_sim.iteritems():
        for strat, info in rest.iteritems():
            info['payoff'].addn(0, numf - info['payoff'].n)
            for feat, val in info['features'].iteritems():
                val.addn(0, numf - val.n)
    # This adds a zero entry for every feature that had no values during a
    # single run
    for feat, mean in features.iteritems():
        mean.addn(0, numf - mean.n)

    return {
        'payoff': payoffs,
        'payoff_sim': payoffs_sim,
        'config': configuration,
        'features': features
    }


if __name__ == "__main__":
    args = parser.parse_args()
    json.dump(mergeObs(args.files), args.output, cls=ObsEncoder)
    args.output.write('\n')
    args.output.close()
