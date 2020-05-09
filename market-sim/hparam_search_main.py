""" Script that bulk submits jobs for hparam searching. """
import itertools
import random
import subprocess
import time
from collections import OrderedDict
from typing import Dict, Sequence

import argparse
import sys
import json
import os
import subprocess
import random

def create_parser():
    parser = argparse.ArgumentParser(description="""Run market-sim with a
            benchmark manipulator who uses DDPG to make its trading decisions,
            where DDPG trains externally of market-sim.""")
    parser.add_argument('--configuration-file', '-c',
            metavar='<configuration-file>', type=argparse.FileType('r'),
            default=sys.stdin, help="""Json file with the hparam configuration.
           	(default: stdin)""")

    return parser


def submit_jobs(submit_template: str, experiment_template: str, name: str, options: Dict[str, Sequence], num_jobs: int=1, num_parallel: int=1):
    """ Perform a random hyperparameter search for an experiment.
    Runs a hyperparameter search for an experiment. A default experimental gin configuration
    must be specified via the template job script, and then the hparam search options are used
    as overrides of the default experiment.
    Expects the experiment to take the following flags:
        experiment_name
        result_dir
        config_dir
        config_files
        config_overrides
    Args:
        submit_template: Template for launching a single job. Must have a `job_id` fied.
            "./slurm_config.sh ..."
        experiment_template: Template for a single experiment/job. 
            "python main.py --result_dir=... --config_dir=... --config_files=..."
        name: Experiment name.        
        options: Options for overriding. The keys are the gin config to override, and the values
            are a list of possible values for the respective key.
        num_parallel: Maximum number of jobs to run in parallel.
    """
    submitted_jobs = set()
    # Generate all of the job submission commands.
    jobs = []
    while len(jobs) < num_jobs:
        # Generate a single run's configuration.
        config = OrderedDict()
        for key, value in options.items():        
            config[key] = random.choice(value)
        # Ensure that this configuration has not been previously launched.
        config_hash = '_'.join([str(x) for x in config.values()])
        if config_hash in submitted_jobs:
            continue
        else:
            submitted_jobs.add(config_hash)
        # Construct job string.
        job_name = f"{name}_{len(jobs)}"
        job = experiment_template
        model_folder_string = "HSLN_env1-"+str(len(jobs))
        conf_file_string = "drl_env1/drl_param-"+str(len(jobs))+".json"
        out_file_string = "drl_env1/output-"+str(len(jobs))
        job += f" -f {model_folder_string} -p {conf_file_string} -o {out_file_string}"
        conf_file= open(conf_file_string,"w")
        json.dump(config, conf_file, sort_keys=True)
        conf_file.close()
        jobs += [(job_name, job)]
    # Launch all of the jobs.
    dependencies = [None] * num_parallel
    for index, (job_name, job) in enumerate(jobs):
        submission = "sbatch"
        # Add the necesssary slurm flags.
        submission += f" --job-name={job_name}"
        # Maybe add the dependency information.
        if index >= num_parallel:  
            dependency = dependencies[index % num_parallel]
            submission += f" --depend=afterany:{dependency}"
        # Add the experiment's executable and flags.        
        submission += f" {submit_template}"
        submission += f" \"{job}\""
        # Launch job.
        _, output = subprocess.getstatusoutput(submission)
        _, _, _, job_num = output.split(' ')
        dependencies[index % num_parallel] = job_num
        print(f"{job_num}: {submission}")
        # Pause between submissions, to go easy on scheduler.
        time.sleep(1)
        #print(submission)

def main():
    """ Runs hyperparameter search. """
    args = create_parser().parse_args()
    conf = json.load(args.configuration_file)
    submit_jobs(conf['submit_template'], conf['experiment_template'], conf['name'], conf['options'], conf['num_jobs'], conf['num_parallel'])

if __name__ == "__main__":
	main()
    