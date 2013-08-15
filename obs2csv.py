from sys import argv, stdout
from os import listdir
import json

def feat2dict(feat):
    dic = {}
    for model, data in feat.iteritems():
        # Add underscore for non configurations
        model = model + '_' if model else model
        for key, value in data.iteritems():
            key = model + key
            dic[key] = value
    return dic


def aggregate(out, obses):
    order = []
    for obs in obses:
        feat = feat2dict(obs['features'])
        if not order:
            order = obs['features'][''].keys()
            order += sorted([k for k in feat.keys() if k not in order])
            for field in order:
                out.write(field)
                out.write(',')
            out.write('\n')

        for k in order:
            out.write(str(feat[k]))
            out.write(',')
        out.write('\n')

if __name__ == '__main__':    
    if len(argv) < 2:
        print 'Usage python obs2csv.py [sim directory] > [result].csv'
        print '      python obs2csv.py [sim directory] [result].csv'
        quit(1)

    fol = argv[1]
    out = stdout if len(argv) == 2 else open(argv[2], 'w')

    def obsJson():
        for obs in (o for o in listdir(fol) if 'observation' in o):
            f = open(fol + '/' + obs)
            j = json.load(f)
            f.close()
            yield j

    aggregate(out, obsJson())
