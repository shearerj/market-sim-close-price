#!/usr/bin/env python3
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
    parser.add_argument('--size-replay-buffer', '-n',
            metavar='<size-replay-buffer>', type=int,
            help="""Max size of replay buffer for DDPG. (default:10000)""")
    parser.add_argument('--warmup', '-w',
            metavar='<warmup>', type=int,
            help="""Number of warmup arrivals before training. (default:100)""")
    parser.add_argument('--training_steps', '-t',
            metavar='<training_steps>', type=int,
            help="""Number of training steps for DDPG. (default:100)""")
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
    parser.add_argument('--replay-buffer-file', '-o', metavar='<replay-buffer-file>',
            type=argparse.FileType('w'), default=sys.stdout, help="""File to
            write replay buffer. Each line of the file is a new json object that
            represents an observation of the manipulator, with state, action, and reward.
            (default: stdout)""")

    parser.add_argument('--mode', default='train', type=str, help='support option: train/test')
    parser.add_argument('--hidden1', default=400, type=int, help='hidden num of first fully connect layer')
    parser.add_argument('--hidden2', default=300, type=int, help='hidden num of second fully connect layer')
    parser.add_argument('--rate', default=0.001, type=float, help='learning rate')
    parser.add_argument('--prate', default=0.0001, type=float, help='policy net learning rate (only for DDPG)')
    parser.add_argument('--discount', default=0.99, type=float, help='')
    parser.add_argument('--bsize', default=64, type=int, help='minibatch size')
    parser.add_argument('--rmsize', default=6000000, type=int, help='memory size')
    parser.add_argument('--window_length', default=1, type=int, help='')
    parser.add_argument('--tau', default=0.001, type=float, help='moving average for target network')
    parser.add_argument('--ou_theta', default=0.15, type=float, help='noise theta')
    parser.add_argument('--ou_sigma', default=0.2, type=float, help='noise sigma') 
    parser.add_argument('--ou_mu', default=0.0, type=float, help='noise mu') 
    parser.add_argument('--validate_episodes', default=20, type=int, help='how many episode to perform during validate experiment')
    parser.add_argument('--max_episode_length', default=500, type=int, help='')
    parser.add_argument('--validate_steps', default=2000, type=int, help='how many steps to perform a validate experiment')
    parser.add_argument('--output', default='output', type=str, help='')
    parser.add_argument('--debug', dest='debug', action='store_true')
    parser.add_argument('--init_w', default=0.003, type=float, help='') 
    parser.add_argument('--train_iter', default=200000, type=int, help='train iters each timestep')
    parser.add_argument('--epsilon', default=50000, type=int, help='')
    parser.add_argument('--seed', default=-1, type=int, help='')

    return parser


def main():
    args = create_parser().parse_args()
    replay_buffer_file = args.replay_buffer_file
    conf = json.load(args.configuration)
    roles = conf.pop('roles')
    mix = json.load(args.mixture)
    role_info = sorted((r, roles[r]) + tuple(zip(*sorted(s.items()))) for r, s in mix.items())

    keys = {key.lower(): key for key in conf['configuration']}
    if 'randomseed' in keys:
        seed = int(conf['configuration'][keys['randomseed']])
        rand.seed(seed)
     
    try:
        samp = {role:
                {strat: int(count) for strat, count
                in zip(s, rand.multinomial(c, probs))
                if count > 0}
                for role, c, s, probs in role_info}
        conf['assignment'] = samp
        conf_f= open("run_scripts/drl_conf.json","w")
        json.dump(conf, conf_f, sort_keys=True)
        conf_f.close()

    except BrokenPipeError:
        pass

    s_rb = args.size_replay_buffer
    warmup = args.warmup
    if (warmup >= s_rb): sys.exit(print("Warmup is bigger than replay buffer!!!"))
    curr_arrivals = 0
    replay_buffer = []

    while curr_arrivals < warmup:
        os.system("./market-sim.sh -s run_scripts/drl_conf.json | jq -c \'(.players[] | select (.role | contains(\"bench_mani\")) | .features)\' > drl_out.json")
        with open('drl_out.json') as json_file:
                feat = json.load(json_file)
        arr = feat['arrivals']
        curr_arrivals = curr_arrivals + arr
        print(curr_arrivals)

        obs = feat['rl_observations']
        for i, ob in enumerate(obs):
            replay_buffer.append(ob)


    print("made it to ddpg!")
    print(replay_buffer[4])
    if args.seed > 0:
        np.random.seed(args.seed)
    nb_states = env.observation_space.shape[0] #size of state space
    nb_actions = env.action_space.shape[0] # size of action space
    agent = DDPG(nb_states, nb_actions, args)

    for i in range(args.training_steps):
        # k is the size of the mini batch
        mini_batch = random.choices(replay_buffer, k=2)
        # convert mini batch into np.array where (k, -1), separate state0, action, reward, state1, terminal
        
        agent.update_policy()

        os.system("./market-sim.sh -s run_scripts/drl_conf.json | jq -c \'(.players[] | select (.role | contains(\"bench_mani\")) | .features)\' > drl_out.json")
        with open('drl_out.json') as json_file:
                feat = json.load(json_file)
        arr = feat['arrivals']
        curr_arrivals = curr_arrivals + arr
        print(curr_arrivals)

        obs = feat['rl_observations']
        for i, ob in enumerate(obs):
            #json.dump(ob, replay_buffer_file)
            replay_buffer.append(ob)

        if curr_arrivals > args.size_replay_buffer:
            begin_buffer = curr_arrivals - args.size_replay_buffer
            replay_buffer = replay_buffer[begin_buffer:len(replay_buffer)]
            curr_arrivals = curr_arrivals - begin_buffer

    for i, ob in enumerate(replay_buffer):
        json.dump(ob, replay_buffer_file)


if __name__ == '__main__':
    main()