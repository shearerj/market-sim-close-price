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

if len(argv) < 2:
    print 'Usage python obs2csv.py [sim directory] > [result].csv'
    print '      python obs2csv.py [sim directory] [result].csv'
    quit(1)

fol = argv[1]
out = stdout if len(argv) == 2 else open(argv[2], 'w')
order = []

for obs in (o for o in listdir(fol) if 'observation' in o):
    f = open(fol + '/' + obs)
    j = json.load(f)
    f.close()
    feat = feat2dict(j['features'])
    if not order:
        order = j['features'][''].keys()
        order += sorted([k for k in feat.keys() if k not in order])
        for field in order:
            out.write(field)
            out.write(',')
        out.write('\n')

    for k in order:
        out.write(str(feat[k]))
        out.write(',')
    out.write('\n')
