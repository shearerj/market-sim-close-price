from sys import argv
from os import mkdir, path, system, listdir, remove
from time import time
import json
import merge_obs

presets = ('CENTRALCDA', 'CENTRALCALL', 'TWOMARKET', 'TWOMARKETLA')
specName = 'simulation_spec.json'

def readJson(file):
    with open(file) as f:
        j = json.load(f)
    return j

if __name__ == '__main__':
    if len(argv) < 3:
        print 'usage...'

    fol = argv[1]
    numSim = int(argv[2])
    spec = readJson(path.join(fol, specName))
    seed = long(time())

    # Run all simulations
    for i, preset in enumerate(presets):
        presetFol = path.join(fol, preset)
        if not path.exists(presetFol): mkdir(presetFol)

        for o in listdir(presetFol):
            if 'observation' in o:
                remove(path.join(presetFol, o))
            
        newSpec = spec.copy()
        newSpec['configuration']['presets'] = preset
        newSpec['configuration']['modelNum'] = str(i)
        newSpec['configuration']['randomSeed'] = str(seed)

        with open(path.join(presetFol, specName), 'w') as outfile:
              json.dump(newSpec, outfile)

        print '>> Running Preset:', preset
        system('./run_hft.sh %s %d' %(presetFol, numSim))

    # Merge observations into csv
    with open(path.join(fol, 'results.csv'), 'w') as outfile:
        merge_obs.mergeObsDirectories([path.join(fol, p) for p in presets], outfile)

    # Run DataChecks (doesn't work on my laptop)
    #system("matlab -nodisplay -nojvm -nosplash -r 'addpath analysis; dataChecks %s; quit' > tmp" %path.join(fol, 'results'))
