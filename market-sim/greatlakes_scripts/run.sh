#!/bin/bash

#SBATCH --mail-user=shearerj@umich.edu
#SBATCH --mail-type=BEGIN,END,FAIL,TIME_LIMIT
#SBATCH --output=/home/shearerj/slurm/slurm-%x-%A_%a.out
#SBATCH --error=/home/shearerj/slurm/slurm-%x-%A_%a.err
#SBATCH --nodes=1
#SBATCH --cpus-per-task=1
#SBATCH --ntasks-per-node=1
#SBATCH --mem-per-cpu=6g
#SBATCH --time=3-00:00
#SBATCH --account=wellman1
#SBATCH --partition=standard

echo $1
eval $1

