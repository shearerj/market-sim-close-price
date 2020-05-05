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
    parser.add_argument('--drl-param-file', '-p', metavar='<drl-param-file>',
            type=argparse.FileType('r'), default=sys.stdin, help="""Json file
            with the deep RL parameters. (default: stdin)""")
    parser.add_argument('--state-file', '-f', metavar='<state-file>',
            type=argparse.FileType('r'), default=sys.stdin, help="""Json file
            that contains state. (default: stdin)""")
    parser.add_argument('--nb-states', '-s', metavar='<nb-states>',
            type=int, help="""Size of state space""")
    parser.add_argument('--nb-actions', '-a', metavar='<nb-actions>',
            type=int, help="""Size of action space""")
    parser.add_argument('--model-path', '-m', metavar='<model-path>', 
            help="""Path of saved model""")

    return parser


def main():
    args = create_parser().parse_args()
    drl_args = json.load(args.drl_param_file)
    state = json.load(args.state_file)
    model_path = args.model_path

    agent = DDPG(args.nb_states, args.nb_actions, drl_args)

    try:
        os.path.exists(model_path)
        agent.load_weights(model_path)
        agent.eval()

    except BrokenPipeError:
        pass

    action = agent.select_action(state['state0'])
    print(action[0])


if __name__ == '__main__':
    main()