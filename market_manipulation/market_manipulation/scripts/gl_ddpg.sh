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

conda init
conda activate market2
cd ../market_manipulation/

python3 train_main.py \
    --experiment_name=test_market_sim \
    --result_dir=../results/ \
    --config_dir=./configs/ \
    --config_files=simple_ddpg_train_main_MW