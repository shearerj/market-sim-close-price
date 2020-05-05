
import argparse
import sys
import json
import os
import subprocess
import random

import numpy.random as rand

import numpy as np
from copy import deepcopy
import torch

#from pytorch_ddpg.evaluator import Evaluator
from pytorch_ddpg.ddpg import DDPG
from pytorch_ddpg.util import *


def create_parser():
    parser = argparse.ArgumentParser(description="""Run market-sim with a
            benchmark manipulator who uses DDPG to make its trading decisions,
            where DDPG trains externally of market-sim.""")
    parser.add_argument('--configuration', '-c',
            metavar='<configuration-file>', type=argparse.FileType('r'),
            default=sys.stdin, help="""Json file with the game configuration.
            Must have a root level field `configuration` with the game
            configuration, and a root level field `roles` that contains the
            role counts for the game. (default: stdin)""")
    parser.add_argument('--mixture', '-m', metavar='<mixture-file>',
            type=argparse.FileType('r'), default=sys.stdin, help="""Json file
            with the mixture proportions. Must be a mapping of {role:
                {strategy: probability}}. (default: stdin)""")
    parser.add_argument('--drl-param-file', '-p', metavar='<drl-param-file>',
            type=argparse.FileType('r'), default=sys.stdin, help="""Json file
            with the deep RL parameters. (default: stdin)""")
    parser.add_argument('--replay-buffer-file', '-o', metavar='<replay-buffer-file>',
            type=argparse.FileType('w'), default=sys.stdout, help="""File to
            write replay buffer. Each line of the file is a new json object that
            represents an observation of the manipulator, with state, action, and reward.
            (default: stdout)""")

    return parser


def main():
    args = create_parser().parse_args()
    replay_buffer_file = args.replay_buffer_file
    drl_args = json.load(args.drl_param_file)
    conf = json.load(args.configuration)
    roles = conf.pop('roles')
    mix = json.load(args.mixture)
    role_info = sorted((r, roles[r]) + tuple(zip(*sorted(s.items()))) for r, s in mix.items())

    keys = {key.lower(): key for key in conf['configuration']}
    if 'randomseed' in keys:
        seed = int(conf['configuration'][keys['randomseed']])
        rand.seed(seed)

    print(drl_args)
    rmsize = int(drl_args["rmsize"])
    warmup = int(drl_args["warmup"])
    if (warmup >= rmsize): sys.exit(print("Warmup is bigger than replay buffer!!!"))
    curr_arrivals = 0
    replay_buffer = []

    for i in range(10000):
        print(i)

        os.system("./market-sim.sh 1 | jq -c \'(.players[] | \"\\(.role) \\(.payoff)\"), (.features | .markets[0] | .benchmark), (.features | .total_surplus) \' > test.json")
        
        with open('test.json', 'w') as json_file:
            feat = json.load(json_file)
        #replay_buffer.append(feat)
        json_file.close()
        with open('HSLN_2_output.json', 'w') as file:
            json.dump(feat, file, sort_keys=True)
        file.close()

    

    HSLN_2_output.json
 
if __name__ == '__main__':
    main()