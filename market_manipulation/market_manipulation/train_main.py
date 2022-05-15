""" Main script to train a policy in Market-Sim.

Example usage:
python train_main.py \
    --experiment_name=test_train_main \
    --config_dir=./configs/ \
    --result_dir=./../results/ \
    --config_files=test_train_main
"""
from __future__ import absolute_import, division, print_function

import logging
import os
import os.path as osp
import warnings
from typing import Union

import gin
import numpy as np
from absl import app, flags

# from market_manipulation import experience_parser_TabQ
from market_manipulation import parse_average_payoff as pap
from market_manipulation.agents.tensorflow_agent import TensorFlowAgent
from market_manipulation.experience_parser import SimpleExperienceParser
from market_manipulation.market_simulator import (DummyMarketSimulator,
                                                  MarketSimulator)
from market_manipulation.training_protocol import training_protocol

FLAGS = flags.FLAGS
LOGGER = logging.getLogger(__name__)


def main(argv):
    # Configure information displayed to terminal.
    np.set_printoptions(precision=2)
    warnings.filterwarnings("ignore")

    # Set-up the result directory.
    cwd = os.getcwd()
    result_dir = osp.join(FLAGS.result_dir, FLAGS.experiment_name)
    FLAGS.result_dir = osp.join(cwd, result_dir)
    result_dir_existed = osp.exists(FLAGS.result_dir)
    if not result_dir_existed:
        os.makedirs(FLAGS.result_dir)

    # Set-up logging.
    logger = logging.getLogger("market_manipulation")
    logger.setLevel(logging.DEBUG)
    logger.propagate = False
    logger.handlers = []  # absl has a default handler that we need to remove.
    # logger.propagate = False
    formatter = logging.Formatter("%(asctime)s %(name)s %(levelname)s %(message)s")
    # Log to terminal.
    terminal_handler = logging.StreamHandler()
    terminal_handler.setFormatter(formatter)
    # Log to file.
    file_handler = logging.FileHandler(osp.join(FLAGS.result_dir, "out.log"))
    file_handler.setLevel(logging.INFO)
    file_handler.setFormatter(formatter)
    # Debug output.
    debug_handler = logging.FileHandler(osp.join(FLAGS.result_dir, "debug.log"))
    debug_handler.setLevel(logging.DEBUG)
    debug_handler.setFormatter(formatter)
    # Register handlers.
    logger.addHandler(terminal_handler)
    logger.addHandler(file_handler)
    logger.addHandler(debug_handler)

    if result_dir_existed:
        logger.warning("Result directory already exists. Files may be overwritten.")

    logger.info(f"Saving results to: {FLAGS.result_dir}")

    # Set-up gin configuration.
    config_dir = osp.join(cwd, FLAGS.config_dir)
    gin_files = [osp.join(config_dir, f"{x}.gin") for x in FLAGS.config_files]
    for gin_file in gin_files:
        assert osp.exists(gin_file) and osp.isfile(gin_file), f"Bad config path: {gin_file}"
    gin.parse_config_files_and_bindings(config_files=gin_files, bindings=FLAGS.config_overrides, skip_unknown=False)

    # Save program flags and configuration.
    FLAGS.append_flags_into_file(osp.join(FLAGS.result_dir, "flags.txt"))
    with open(osp.join(FLAGS.result_dir, "config.txt"), "w") as config_file:
        config_file.write(gin.config_str())

    # Run experiment.
    train_policy(
        agent=gin.REQUIRED, market_sim=gin.REQUIRED, experience_parser=gin.REQUIRED, zi_payoff_parser=gin.REQUIRED
    )


@gin.configurable
def train_policy(
    agent: TensorFlowAgent,
    market_sim: Union[MarketSimulator, DummyMarketSimulator],
    experience_parser: SimpleExperienceParser,
    zi_payoff_parser: pap.ParseAveragePayoff,
):
    """Train a policy in Market-Sim.

    Args:
        agent: TensorFlow agent to train.
        market_sim: Market simulator.
        experience_parser: Module for parsing Market-Sim logs into experiences.
        zi_payoff_parser: Module for parsing Market-Sim logs into average ZI payoffs
    """
    LOGGER.info(f"Starting training of {agent}.")
    training_protocol(
        agent=agent,
        agent_name="rl_agent",
        env=market_sim,
        experience_parser=experience_parser,
        zi_payoff_parser=zi_payoff_parser,
        update=True,
        result_dir=FLAGS.result_dir,
    )
    LOGGER.info("Completed.")


if __name__ == "__main__":
    flags.DEFINE_string("experiment_name", None, "Experiment's run name.")
    flags.DEFINE_string("result_dir", None, "Name of directory containing config files.")
    flags.DEFINE_string("config_dir", None, "Name of directory containing config files.")
    flags.DEFINE_multi_string("config_files", None, "Name of the gin config files to use.")
    flags.DEFINE_multi_string("config_overrides", [], "Overrides for gin config values.")
    flags.mark_flag_as_required("experiment_name")

    app.run(main)
