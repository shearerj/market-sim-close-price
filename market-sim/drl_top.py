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

from mse import getDataList, getStats


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
    parser.add_argument('--model-folder', '-f', metavar='<model-folder>',
             default="temp_model", help="""Folder to store model for later testing.""")
    parser.add_argument('--output-file', '-o', metavar='<output-file>',
             default="stdout", help="""File to store stats from testing.""")

    return parser

def writeConfigFile(conf, role_info, nb_states, nb_actions, model_folder, policy_action):
    try:
        samp = {role:
                {strat: int(count) for strat, count
                in zip(s, rand.multinomial(c, probs))
                if count > 0}
                for role, c, s, probs in role_info}
        conf['assignment'] = samp
        if policy_action:
            conf['configuration']['policyAction'] = 'true'
            conf['configuration']['nbStates'] = nb_states
            conf['configuration']['nbActions'] = nb_actions
            conf['configuration']['benchmarkModelPath'] = model_folder
        else:
            conf['configuration']['policyAction'] = 'false'
        conf_f= open("run_scripts/drl_conf.json","w")
        json.dump(conf, conf_f, sort_keys=True)
        conf_f.close()

    except BrokenPipeError:
        pass

def main():
    os.system("touch test2.json")
    args = create_parser().parse_args()
    model_folder = args.model_folder
    output_file = args.output_file
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

    os.system("touch test3.json")
    while curr_arrivals < warmup:
        # Generate new mixed strategies for agents
        writeConfigFile(conf, role_info, 0, 0, model_folder, False)

        os.system("touch test4.json")
        os.system("./market-sim.sh -s run_scripts/drl_conf.json | jq -c \'(.players[] | select (.role | contains(\"bench_mani\")) | .features)\' > drl_out.json")
        with open('drl_out.json') as json_file:
                feat = json.load(json_file)
        arr = feat['arrivals']
        curr_arrivals = curr_arrivals + arr
        print(curr_arrivals)

        obs = feat['rl_observations']
        for i, ob in enumerate(obs):
            if 'price' in ob['action']:
                    replay_buffer.append(ob)
            else:
                curr_arrivals = curr_arrivals - 1
            #replay_buffer.append(ob)
        #curr_arrivals = 4 #This line is for testing to pass warmup loop


    print("made it to ddpg!")
    assert np.array(replay_buffer[0]['state0']).size == np.array(replay_buffer[0]['state1']).size,"First state0 != state1 length."
    assert np.array(replay_buffer[0]['state0']).size == np.array(replay_buffer[len(replay_buffer)-1]['state0']).size,"First state0 != last state0 length."
    assert np.array(replay_buffer[len(replay_buffer)-1]['state0']).size == np.array(replay_buffer[len(replay_buffer)-1]['state1']).size,"Last state0 != state1 length."
    assert np.array(replay_buffer[0]['action']).size == np.array(replay_buffer[len(replay_buffer)-1]['action']).size,"First action != last action length."
    nb_states = np.array(replay_buffer[0]['state0']).size
    nb_actions = np.array(replay_buffer[0]['action']).size

    if int(drl_args["seed"]) > 0:
        np.random.seed(int(drl_args["seed"]))
    agent = DDPG(nb_states, nb_actions, drl_args)

    bsize = int(drl_args["bsize"])
    print(len(replay_buffer))
    assert bsize <= len(replay_buffer), "Mini batch is bigger than replay buffer."

    if not os.path.exists(model_folder):
        os.makedirs(model_folder)
        with open(".gitignore", "a") as ignore_file:
            ignore_file.write('\n/'+model_folder+'/*')

    training_steps = int(drl_args["trainingSteps"])
    for i in range(training_steps):
        print("Training step "+str(i))
        mini_batch = random.choices(replay_buffer, k=bsize)
        state0_batch = np.empty((bsize,nb_states))
        state1_batch = np.empty((bsize,nb_states))
        action_batch = np.empty((bsize,nb_actions))
        term_batch = np.empty((bsize,1))
        reward_batch = np.empty((bsize,1))
        for j, ob in enumerate(mini_batch):
            state0_batch[j,:] = np.array(ob['state0'])
            state1_batch[j,:] = np.array(ob['state1'])
            action_batch[j,:] = np.array(ob['action']['price'])
            term_batch[j,:] = np.array(ob['terminal'])
            reward_batch[j,:] = np.array(ob['reward'])
        
        # Update the policy the number of arrivals
        for j in range(int(drl_args["updateSteps"])):
            agent.update_policy(state0_batch, action_batch, reward_batch, state1_batch, term_batch)

        agent.save_model(model_folder)

        if i < training_steps - 1:
            # get weights and bias for policy actions
            # create new config file where update policy action to true, and feed in weights etc

            # Generate new mixed strategies for agents
            writeConfigFile(conf, role_info, nb_states, nb_actions, model_folder, True)

            os.system("./market-sim.sh -s run_scripts/drl_conf.json | jq -c \'(.players[] | select (.role | contains(\"bench_mani\")) | .features)\' > train_out.json")
            with open('train_out.json') as json_file:
                    feat = json.load(json_file)
            arr = feat['arrivals']
            curr_arrivals = curr_arrivals + arr
            print(curr_arrivals)
            json_file.close()

            obs = feat['rl_observations']
            for i, ob in enumerate(obs):
                # Hacky way to vefiy that action was selected
                if 'price' in ob['action']:
                #json.dump(ob, replay_buffer_file)
                    replay_buffer.append(ob)
                else:
                    curr_arrivals = curr_arrivals - 1

            if curr_arrivals > rmsize:
                begin_buffer = curr_arrivals - rmsize
                replay_buffer = replay_buffer[begin_buffer:len(replay_buffer)]
                curr_arrivals = curr_arrivals - begin_buffer

    # testing
    stats = []
    testing_steps = int(drl_args["testingSteps"])
    for i in range(testing_steps):
        writeConfigFile(conf, role_info, nb_states, nb_actions, model_folder, True)
        os.system("./market-sim.sh -s run_scripts/drl_conf.json | jq -c  '(.players[] | \"\(.role) \(.payoff)\"), (.features | .markets[0] | .benchmark), (.features | .total_surplus) ' > test_out.json")
        #os.system("./market-sim.sh -s run_scripts/drl_conf.json | jq -c  '(.players[]), (.features) ' > test_out.json")
        with open('test_out.json') as f:
            lines = [line.replace('\"', '').split() for line in f]
        f.close()
        for j,s in enumerate(lines):
            stats.append(s)

    with open(output_file, "w") as output:
        output.write(str(stats))
    output.close()

    num_agents = 0
    for key,value in roles.items():
        num_agents += value
    zi, bm, bmM, ts, tsM, h_zi, ah_zi, h_bm, ah_bm, bmB = getDataList(stats,num_agents)
    print('ZI')
    getStats(zi)
    print('Benchmark Manipulator')
    getStats(bm)
    print('Benchmark Manipulator Market Performance')
    getStats(bmM)
    print('Benchmark Manipulator Benchmark Performance')
    getStats(bmB)
    print('Total Surplus')
    getStats(ts)
    print('Market Surplus')
    getStats(tsM)

    #for i, ob in enumerate(replay_buffer):
        #json.dump(ob, replay_buffer_file)


if __name__ == '__main__':
    main()
