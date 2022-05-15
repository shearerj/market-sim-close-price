#!/bin/bash
source activate market_manipulation
cd ../market_manipulation/

python3 train_main.py \
    --experiment_name=test_market_sim \
    --result_dir=../results/ \
    --config_dir=./configs/ \
    --config_files=bench_ddpg_train_main_MW