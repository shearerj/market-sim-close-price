from sys import argv, stdout
import json
import obs2csv
from os import listdir

def commonConfig(configs):
    return dict(set.intersection(*[set(c.items()) for c in configs]))

def readJson(file):
    f = open(file)
    j = json.load(f)
    f.close()
    return j

def mergeObs(obs):
    obs = [o['features'] for o in obs]
    features = {}
    features[''] = commonConfig([o[''] for o in obs])
    for o in obs:
        name = o['']['modelName'] + '_'
        for k,v in [(k,v) for k,v in o.iteritems() if k != '']:
            features[name + k] = v
    return {'assignment':[], 'features':features}

if __name__ == '__main__':
    dirs = argv[1:]
    unique = set.intersection(*[set(f for f in listdir(d) if 'observation' in f) for d in dirs])
    def doMerge():
        for obs in unique:
            yield mergeObs([readJson(d + '/' + obs) for d in dirs])
    obs2csv.aggregate(stdout, doMerge())
