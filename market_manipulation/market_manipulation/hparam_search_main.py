""" Script that bulk submits jobs for hparam searching. """
import itertools
import random
import subprocess
import time
from collections import OrderedDict
from typing import Dict, Sequence

import gin
from absl import app, flags


@gin.configurable
def submit_jobs(
    submit_template: str,
    experiment_template: str,
    name: str,
    options: Dict[str, Sequence],
    num_jobs: int = 1,
    num_parallel: int = 1,
):
    """Perform a random hyperparameter search for an experiment.

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
        config_hash = "_".join([str(x) for x in config.values()])
        if config_hash in submitted_jobs:
            continue
        else:
            submitted_jobs.add(config_hash)

        # Construct job string.
        job_name = f"{name}_{len(jobs)}"
        job = experiment_template
        job += f" --experiment_name={job_name}"
        for key, value in config.items():
            if isinstance(value, str):
                job += f" --config_overrides='{key}=\\\"{value}\\\"'"

            elif isinstance(value, list):
                job += f" --config_overrides='{key}=["
                for subvalue in value:
                    if isinstance(subvalue, str):
                        job += f'\\"{subvalue}\\",'
                    else:
                        job += f"{subvalue},"
                job += "]'"

            else:
                job += f" --config_overrides='{key}={value}'"

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
        submission += f' "{job}"'

        # Launch job.
        _, output = subprocess.getstatusoutput(submission)
        _, _, _, job_num = output.split(" ")
        dependencies[index % num_parallel] = job_num
        print(f"{job_num}: {submission}")
        # Pause between submissions, to go easy on scheduler.
        time.sleep(1)


def main(argv):
    """Runs hyperparameter search."""
    gin.parse_config_file(FLAGS.config_file)
    submit_jobs()


if __name__ == "__main__":
    flags.DEFINE_string("config_file", None, "Name of the gin config containing search settings.")
    flags.mark_flag_as_required("config_file")
    FLAGS = flags.FLAGS
    app.run(main)
