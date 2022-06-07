#!/usr/bin/env python3
import argparse
import sys
import json
import os
import subprocess
import random

import numpy.random as rand

import numpy as np


def create_parser():
    parser = argparse.ArgumentParser(description="""Run market-sim multiple times
            while resampling mixed profiles.""")
    parser.add_argument('--num-runs', '-n',
            metavar='<num-runs>', type=int,
            help="""Number of times to run market-sim. (default:100)""")
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
    parser.add_argument('--output-file', '-o', metavar='<output-file>',
            type=argparse.FileType('w'), default=sys.stdout, help="""File to
            write market-sim output to.
            (default: stdout)""")
    parser.add_argument('--fixed-seed', '-s', metavar='<fixed-seed>',
            type=bool, default=False, help="""Determines if a fixed seed
            is used for each profile generation and run. (default: false)""")

    return parser


def main():
    args = create_parser().parse_args()
    num_runs = args.num_runs
    fixed_seed = args.fixed_seed
    output_file = args.output_file
    output_file = args.output_file
    conf = json.load(args.configuration)
    roles = conf.pop('roles')
    mix = json.load(args.mixture)
    role_info = sorted((r, roles[r]) + tuple(zip(*sorted(s.items()))) for r, s in mix.items())

    memory_buffer = {}

    background_payoff = []
    bench_total_payoff =[]
    bench_market_payoff = []
    total_payoff = []
    market_payoff = []
    benchmark = []


    for i in range(num_runs):
        if fixed_seed: rand.seed(i)
        # Sample new mixture of agents
        try:
            samp = {role:
                    {strat: int(count) for strat, count
                    in zip(s, rand.multinomial(c, probs))
                    if count > 0}
                    for role, c, s, probs in role_info}
            conf['assignment'] = samp
            if fixed_seed: conf['configuration']['randomseed'] = i
            conf_f= open("run_scripts/temp_conf.json","w")
            json.dump(conf, conf_f, sort_keys=True)
            conf_f.close()
        
        except BrokenPipeError:
            pass

        # Run market-sim
        os.system("java -jar target/marketsim-4.0.0-jar-with-dependencies.jar -s run_scripts/temp_conf.json | jq . > temp_out.json")
        with open('temp_out.json') as json_file:
                feat = json.load(json_file)

        # Store wanted stats in memory_buffer
        b = feat['features']['markets'][0]['benchmark']
        benchmark.append(b)
        play = feat['players']
        tp = 0
        for i, p in enumerate(play):
            if p['role'] == 'zi':
                background_payoff.append(p['payoff'])
            if p['role'] == 'benchmark':
                bench_total_payoff.append(p['payoff'])
                bench_market_payoff.append(p['payoff'] - 40 * b)
            tp = tp + p['payoff']
        total_payoff.append(tp)
        market_payoff.append(tp - 40 * b)
                    

    memory_buffer['ZI Payoff'] = {'Mean': np.mean(background_payoff), 'Variance': np.var(background_payoff)}
    memory_buffer['Benchmark Total Payoff'] = {'Mean': np.mean(bench_total_payoff), 'Variance': np.var(bench_total_payoff)}
    memory_buffer['Benchmark Market Payoff'] = {'Mean': np.mean(bench_market_payoff), 'Variance': np.var(bench_market_payoff)}
    memory_buffer['Total Payoff'] = {'Mean': np.mean(total_payoff), 'Variance': np.var(total_payoff)}
    memory_buffer['Market Payoff'] = {'Mean': np.mean(market_payoff), 'Variance': np.var(market_payoff)}
    memory_buffer['Benchmark'] = {'Mean': np.mean(benchmark), 'Variance': np.var(benchmark)}
    
    json.dump(memory_buffer, output_file)


if __name__ == '__main__':
    main()