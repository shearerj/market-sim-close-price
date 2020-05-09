#!/bin/bash

#SBATCH --mail-user=shearerj@umich.edu
#SBATCH --mail-type=BEGIN,END,FAIL,TIME_LIMIT
#SBATCH --output=/home/shearerj/slurm/slurm-%x-%A_%a.out
#SBATCH --error=/home/shearerj/slurm/slurm-%x-%A_%a.err
#SBATCH --nodes=1
#SBATCH --cpus-per-task=1
#SBATCH --ntasks-per-node=1
#SBATCH --mem-per-cpu=20g
#SBATCH --time=1-00:00
#SBATCH --account=wellman1
#SBATCH --partition=standard

touch test1.json
#python3 drl_top.py -c run_scripts/HSLN_drl.json -m profiles/mani_HSLN_drl.json  -p run_scripts/drl_param.json -f test_model -o test_gl_out.txt
python3 drl_top.py -c drl_env1/ms_config.json -m drl_env1/profile.json -f HSLN_env1-0 -p drl_param-0.json -o drl_env1/output-0
