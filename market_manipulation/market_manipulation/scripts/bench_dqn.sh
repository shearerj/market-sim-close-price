#!/bin/bash
cd ../market_manipulation/

python3 train_main.py \
    --experiment_name=test_market_sim \
    --result_dir=../results/ \
    --config_dir=./configs/ \
    --config_files=bench_dqn_train_main_MW