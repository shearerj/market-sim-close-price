#!/usr/bin/env python3
import argparse
import sys
import json
import os
import subprocess
import random
import matplotlib.pyplot as plt

import numpy.random as rand

import numpy as np
from copy import deepcopy
import torch

#from pytorch_ddpg.evaluator import Evaluator
from pytorch_ddpg.ddpg import DDPG
from pytorch_ddpg.util import *

from mse import getDataList, getStats, getStatsJson


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
    parser.add_argument('--state-flags-file', '-s', metavar='<state-flags-file>',
            type=argparse.FileType('r'), default=sys.stdin, help="""Json file
            with the state space flags. (default: stdin)""")
    parser.add_argument('--job_num', '-j', metavar='<job_num>',
            type=int, default=-1, help="""The job number for greatlakes. (default: -1)""")
    parser.add_argument('--model-folder', '-f', metavar='<model-folder>',
             default="temp_model", help="""Folder to store model for later testing.""")
    parser.add_argument('--output-file', '-o', metavar='<output-file>',
             default="stdout", help="""File to store stats from testing.""")

    return parser

def writeConfigFile(conf, role_info, nb_states, nb_actions, model_folder, config_path, actor_weights = {}, policy_action = False, isTraining = False):
    try:
        samp = {role:
                {strat: int(count) for strat, count
                in zip(s, rand.multinomial(c, probs))
                if count > 0}
                for role, c, s, probs in role_info}
        conf['assignment'] = samp
        conf['configuration']['actorWeights'] = str(actor_weights)
        conf['configuration']['isTraining'] = isTraining
        if policy_action:
            conf['configuration']['policyAction'] = 'true'
            conf['configuration']['nbStates'] = nb_states
            conf['configuration']['nbActions'] = nb_actions
        else:
            conf['configuration']['policyAction'] = 'false'
        conf_f= open(config_path,"w")
        json.dump(conf, conf_f, sort_keys=True)
        conf_f.close()

    except BrokenPipeError:
        pass

def main():
    args = create_parser().parse_args()
    model_folder = args.model_folder
    output_file = args.output_file
    drl_param_file = args.drl_param_file
    state_flags = json.load(args.state_flags_file)
    drl_args = json.load(drl_param_file)
    conf = json.load(args.configuration)
    config_split = args.configuration.name.split('.')
    job_num = args.job_num
    if (job_num >= 0): config_path = config_split[0] + '_' + str(job_num) + '.' + config_split[1]
    else: config_path = 'run_scripts/drl_conf.json'
    roles = conf.pop('roles')
    mix = json.load(args.mixture)
    role_info = sorted((r, roles[r]) + tuple(zip(*sorted(s.items()))) for r, s in mix.items())

    conf['configuration']['hiddenLayer1'] = drl_args['hidden1']
    conf['configuration']['hiddenLayer2'] = drl_args['hidden2']
    conf['configuration']['actionCoefficient'] = drl_args['actionCoefficient']
    conf['configuration']['viewBookDepth'] = drl_args['viewBookDepth']
    conf['configuration']['omegaDepth'] = drl_args['omegaDepth']
    conf['configuration']['transactionDepth'] = drl_args['transactionDepth']
    #conf['configuration']['benchmarkModelPath'] = model_folder
    #conf['configuration']['benchmarkParamPath'] = drl_param_file.name
    #conf['configuration']['greatLakesJobNumber'] = job_num
    conf['configuration']['stateSpaceFlags'] = str(state_flags)
    conf['configuration']['OUMu'] = drl_args['ou_mu']
    conf['configuration']['OUSigma'] = drl_args['ou_sigma']
    conf['configuration']['OUTheta'] = drl_args['ou_theta']
    print(drl_args['epsilon'])
    conf['configuration']['EpsilonDecay'] = drl_args['epsilon']

    keys = {key.lower(): key for key in conf['configuration']}
    if 'randomseed' in keys:
        seed = int(conf['configuration'][keys['randomseed']])
        rand.seed(seed)

    rmsize = int(drl_args["rmsize"])
    warmup = int(drl_args["warmup"])
    if (warmup >= rmsize): sys.exit(print("Warmup is bigger than replay buffer!!!"))
    curr_arrivals = 0
    replay_buffer = []

    temp_out_path = f'{output_file}_temp.json'

    while curr_arrivals < warmup:
        # Generate new mixed strategies for agents
        writeConfigFile(conf, role_info, 0, 0, model_folder, config_path)

        #  print(f"./market-sim.sh -s {config_path} | jq -c \'(.players[] | select (.role | contains(\"bench_mani\")) | .features)\' > {temp_out_path}")

        os.system(f"./market-sim.sh -s {config_path} | jq -c \'(.players[] | select (.role | contains(\"bench_mani\")) | .features)\' > {temp_out_path}")
        with open(temp_out_path) as json_file:
                feat = json.load(json_file)
        arr = feat['arrivals']
        curr_arrivals = curr_arrivals + arr
        print(curr_arrivals)

        obs = feat['rl_observations']
        for i, ob in enumerate(obs):
            if len(ob['action']) > 0:
                ob['action'] =  ob['action']['alpha']
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

    #if not os.path.exists(model_folder):
    #    os.makedirs(model_folder)
    #    with open(".gitignore", "a") as ignore_file:
    #        ignore_file.write('\n/'+model_folder+'/*')

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
            action_batch[j,:] = np.array(ob['action'])
            term_batch[j,:] = np.array(ob['terminal'])
            reward_batch[j,:] = np.array(ob['reward'])
        
        # Update the policy the number of arrivals
        for j in range(int(drl_args["updateSteps"])):
            agent.update_policy(state0_batch, action_batch, reward_batch, state1_batch, term_batch)

        #agent.save_model(model_folder)

        actor_weights = {}
        actor_weights['weightMtx1'] = to_numpy(agent.actor.fc1.weight).tolist()
        actor_weights['biasMtx1'] = to_numpy(agent.actor.fc1.bias).tolist()
        actor_weights['weightMtx2'] = to_numpy(agent.actor.fc2.weight).tolist()
        actor_weights['biasMtx2'] = to_numpy(agent.actor.fc2.bias).tolist()
        actor_weights['weightMtx3'] = to_numpy(agent.actor.fc3.weight).tolist()
        actor_weights['biasMtx3'] = to_numpy(agent.actor.fc3.bias).tolist()

        if i < training_steps - 1:
            # get weights and bias for policy actions
            # create new config file where update policy action to true, and feed in weights etc

            # Generate new mixed strategies for agents
            writeConfigFile(conf, role_info, nb_states, nb_actions, model_folder, config_path, actor_weights = actor_weights, policy_action = True, isTraining = True)

            os.system(f"./market-sim.sh -s {config_path} | jq -c \'(.players[] | select (.role | contains(\"bench_mani\")) | .features)\' > {temp_out_path}")
            with open(temp_out_path) as json_file:
                    feat = json.load(json_file)
            arr = feat['arrivals']
            curr_arrivals = curr_arrivals + arr
            print(curr_arrivals)
            json_file.close()

            obs = feat['rl_observations']
            for i, ob in enumerate(obs):
                # Hacky way to vefiy that action was selected
                if len(ob['action']) > 0:
                #json.dump(ob, replay_buffer_file)
                    ob['action'] =  ob['action']['alpha']
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
        writeConfigFile(conf, role_info, nb_states, nb_actions, model_folder, config_path, actor_weights = actor_weights, policy_action = True)
        os.system(f"./market-sim.sh -s {config_path} | jq -c  '(.players[] | \"\(.role) \(.payoff)\"), (.features | .markets[0] | .benchmark), (.features | .total_surplus) ' > {temp_out_path}")
        #os.system("./market-sim.sh -s run_scripts/drl_conf.json | jq -c  '(.players[]), (.features) ' > test_out.json")
        with open(temp_out_path) as f:
            lines = [line.replace('\"', '').split() for line in f]
        f.close()
        for j,s in enumerate(lines):
            stats.append(s)

    #print("actor loss")
    prate = drl_args['prate']
    rate = drl_args['rate']
    plt.title(f'Actor Loss, prate={prate}, rate={rate}, bsize={bsize}')
    plt.plot(agent.getActorLoss())
    plt.savefig(f'{output_file}_actor_loss.png')
    plt.close()
    #print("critic loss")
    plt.title(f'Critic Loss, prate={prate}, rate={rate}, bsize={bsize}')
    plt.plot(agent.getCriticLoss())
    plt.savefig(f'{output_file}_critic_loss.png')
    plt.close()

    num_agents = 0
    for key,value in roles.items():
        num_agents += value
    zi, bm, bmM, ts, tsM, h_zi, ah_zi, h_bm, ah_bm, bmB = getDataList(stats,num_agents)
    print('ZI\n')
    print(getStats(zi))
    print('Benchmark Manipulator\n')
    print(getStats(bm))
    print('Benchmark Manipulator Market Performance\n')
    print(getStats(bmM))
    print('Benchmark Manipulator Benchmark Performance\n')
    print(getStats(bmB))
    print('Total Surplus\n')
    print(getStats(ts))
    print('Market Surplus\n')
    print(getStats(tsM))

    stats_condensed = {}
    stats_condensed['ZI'] = getStatsJson(zi)
    stats_condensed['Benchmark-Manipulator'] = getStatsJson(bm)
    stats_condensed['Benchmark-Manipulator-Market-Performance'] = getStatsJson(bmM)
    stats_condensed['Benchmark-Manipulator-Benchmark-Performance'] = getStatsJson(bmB)
    stats_condensed['Total-Surplus'] = getStatsJson(ts)
    stats_condensed['Market-Surplus'] = getStatsJson(tsM)

    output= open(output_file + '.json',"w")
    json.dump(stats_condensed, output)
    output.close()

    with open(output_file + '_full.txt', "w") as output:
        output.write(str(stats))
    output.close()

    #for i, ob in enumerate(replay_buffer):
        #json.dump(ob, replay_buffer_file)


if __name__ == '__main__':
    main()
