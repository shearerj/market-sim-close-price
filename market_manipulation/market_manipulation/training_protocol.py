""" A simple agent-environment interaction protocol for the Market-Sim Java environment. """
import logging
import os
import os.path as osp
import shutil
from typing import Any, Callable, Sequence

import gin
import numpy as np
from torch.utils.tensorboard import SummaryWriter

import market_manipulation.utils.early_stopping as early_stopping
from market_manipulation.utils.dummy_file_writer import DummyFileWriter

LOGGER = logging.getLogger(__name__)


@gin.configurable  # noqa: C901
def training_protocol(
    agent: Any,
    debug_mode: bool,
    reward_shape: bool,
    agent_name: str,
    agent_type: str,
    env: Any,
    experience_parser: Any,
    zi_payoff_parser: Any,
    validation_timesteps: int,
    validation_loop_steps: int,
    load_model: bool,
    load_model_path: str,
    agent_reward_clip_cutoff: float = 1.0,
    num_episodes: int = None,
    num_timesteps: int = None,
    exploitation: bool = False,
    update: bool = False,
    result_dir: str = None,
    hooks: Sequence = [],
    early_stopping_fn: Callable = early_stopping.never,
    exploitation_steps: int = 100,
    agent_max_vector_depth: int = 20,
    agent_bid_len: int = 5,
    agent_ask_len: int = 5,
    agent_trade_len: int = 5,
    agent_additional_actions: str = "rmin/rmax/thresh",
) -> None:
    """Runs an agent on an environment.

    Args:
        agent: The agent to train and evaluate.
        agent_name: string used to describe agent name (exp 'ZI')
        env: The environment to train on.
        experience_parser: Class that will parse experience from market-sim runs (exp experience_parser.py)
        zi_payoff_parser: Class that will parse the average payoff from ZI agents in a market-sim run
        num_episodes: Number of episodes to train for.
        num_timesteps: Number of timesteps to train for.
        update: Whether to update the agent's behavior.
        result_dir: Directory for results of training
        hooks:
            NOTE: Before/after timestep hooks are not supported.
        early_stopping_fn: Function that can handle early stopping in training
        agent_max_vector_depth: max fixed length of any vector in market-sim (exp. bid vector)
        agent_additional_actions: tensorflow graph outputs that are expected in addition to price/side/size
    """
    # Check that the specified termination conditions are valid.
    assert (
        num_episodes is not None and num_timesteps is None
    ), "Timestep level termination is not support in Market-Sim."

    # Set-up a Tensorboard result writer.
    if result_dir is not None:
        writer = SummaryWriter(log_dir=result_dir)
    else:
        writer = DummyFileWriter(logdir="")  # Doesn't use `logdir`.

    # Create a file to dump qtable data
    # f = open(osp.join(result_dir, "qtable.txt"), mode='w')

    # Perform any pre-training hook operations.
    for hook in hooks:
        hook.begin(agent=agent, env=env, result_dir=result_dir, update=update)

    total_episodes = 0
    validation_episodes = 0
    episodic_rewards = []

    # Create a folder to hold the output logs for summary statistics
    log_path = osp.join(result_dir, "market_log")
    if osp.exists(log_path):
        shutil.rmtree(log_path)
        os.makedirs(log_path)
    else:
        os.makedirs(log_path)

    agent.set_market_vec_len(agent_bid_len, agent_ask_len, agent_trade_len, agent_max_vector_depth)
    while True:
        # If exploiting an already trained policy, break out of loop immediately
        if load_model:
            break

        # Perform any pre-epsidoe hook operations.
        for hook in hooks:
            hook.before_episode()

        # Save policy to be loaded in Java.
        policy_save_path = osp.join(result_dir, "saved_agent.pb")
        agent.save(policy_save_path)

        # Run Market-Sim.
        simulation_results = env.simulate(
            agent_name=agent_name,
            model_path=policy_save_path,
            agent_max_vector_depth=agent_max_vector_depth,
            agent_additional_actions=agent_additional_actions,
            validation_mode=False,
            load_mode=False,
            load_path="",
        )

        # Parse the Market-Sim results and add the experiences to the agent's replay buffer.
        experiences, payoff, actions = experience_parser(
            agent_type,
            agent_name,
            simulation_results,
            debug_mode,
            reward_shape,
            agent_bid_len,
            agent_ask_len,
            agent_trade_len,
            agent_max_vector_depth,
        )
        print(payoff)
        print(actions)
        writer.add_histogram("agent/action_dist", np.array(actions))
        zi_payoff = zi_payoff_parser("background", simulation_results)
        # payoff = sum([e[3] for e in experiences])
        agent.record_experiences(experiences)

        if debug_mode:
            # Log rewards
            with open(osp.join(log_path, f"rewards_{agent_type}.txt"), mode="a+") as r_file:
                for e in experiences:
                    r_file.write(str(e[4]) + ",")
                r_file.truncate(r_file.tell() - 1)
                r_file.write("\n")

            # Log action chosen
            with open(osp.join(log_path, f"action_dist_{agent_type}.txt"), mode="a+") as a_file:
                for e in experiences:
                    a_file.write(str(e[1]) + ",")
                a_file.truncate(a_file.tell() - 1)
                a_file.write("\n")

            if agent_type == "DQN":
                with open(osp.join(log_path, f"qval_dist_{agent_type}.txt"), mode="a+") as qval_file:
                    for e in experiences:
                        qval_file.write(str(e[-1]) + ",")
                    qval_file.truncate(qval_file.tell() - 1)
                    qval_file.write("\n")

        if (agent_type in ["DDPG", "DDPG_BENCH"]) and agent_reward_clip_cutoff * num_episodes < total_episodes:
            agent.set_reward_clipping(False)

        if update:
            # Perform a training update and log any resulting statistics received.
            if agent_type == "TabQ":
                if debug_mode:
                    log, qtable = agent.update()
                    # Log the final q table
                    with open(osp.join(result_dir, "qtable.npy"), mode="wb") as q_file:
                        np.save(q_file, qtable)
                else:
                    log = agent.update()
            elif agent_type in ["DQN", "DDPG", "DQN_BENCH", "DDPG_BENCH"]:
                log = agent.update()
            else:
                print("Invalid agent type")
                assert False

            # write to the tensorboard
            for key, value in log.items():
                writer.add_scalar(f"agent/{key}", value, total_episodes)

        # Perform any post-episode hook operations.
        for hook in hooks:
            hook.after_episode(timestep=None)

        # Report the average return of the last 100 episodes.
        episodic_rewards += [payoff]
        if len(episodic_rewards) > 100:
            episodic_rewards = episodic_rewards[-100:]
        writer.add_scalar("mean_return", np.mean(episodic_rewards), total_episodes)
        writer.add_scalar("return", episodic_rewards[-1], total_episodes)

        # Report the average payoff of the ZI agents playing against TFRL
        writer.add_scalar("Background trader average Payoff", zi_payoff, total_episodes)

        # Report the payoff of the tfrl agent
        writer.add_scalar("TFRL payoff", payoff, total_episodes)

        # Update and check terimation conditions.
        total_episodes += 1
        if total_episodes >= num_episodes:
            break

        if total_episodes % validation_timesteps == 0:
            print("VALIDATING")
            validation_episodes += 1
            payoff_list_zi = []
            payoff_list_tfrl = []
            val_actions = []
            if agent_type not in ["DDPG", "DDPG_BENCH"]:
                agent.reset_epsilon()
            agent.save(policy_save_path)
            for i in range(validation_loop_steps):
                # Run market-sim with a fixed policy
                simulation_results = env.simulate(
                    agent_name=agent_name,
                    model_path=policy_save_path,
                    agent_max_vector_depth=agent_max_vector_depth,
                    agent_additional_actions=agent_additional_actions,
                    validation_mode=True,
                    load_mode=False,
                    load_path="",
                )
                experiences, payoff, actions = experience_parser(
                    agent_type,
                    agent_name,
                    simulation_results,
                    debug_mode,
                    reward_shape,
                    agent_bid_len,
                    agent_ask_len,
                    agent_trade_len,
                    agent_max_vector_depth,
                )
                print(payoff)
                print(actions)
                val_actions.extend(actions)
                zi_payoff = zi_payoff_parser("background", simulation_results)
                tfrl_payoff = zi_payoff_parser(agent_name, simulation_results)
                payoff_list_zi.append(zi_payoff)
                payoff_list_tfrl.append(tfrl_payoff)

            # After loop, record average to the tensorboard
            writer.add_scalar(
                "agent/validation/background_payoff", np.mean(np.array(payoff_list_zi)), validation_episodes
            )
            writer.add_scalar("agent/validation/tfrl", np.mean(np.array(payoff_list_tfrl)), validation_episodes)
            writer.add_scalar(
                "agent/validation/background_payoff_std", np.std(np.array(payoff_list_zi)), validation_episodes
            )
            writer.add_scalar("agent/validation/tfrl_std", np.std(np.array(payoff_list_tfrl)), validation_episodes)

            writer.add_histogram("agent/validation/action_dist", np.array(val_actions))

            # Set the epsilon value back to what it was originally
            if agent_type not in ["DDPG", "DDPG_BENCH"]:
                agent.set_epsilon()

        if early_stopping_fn(performance=payoff):
            LOGGER.info("Stopping training early.")
            break

    # Perform any post-training hook operations.
    for hook in hooks:
        hook.end()

    # Final trained policy exploitation
    if exploitation:
        if load_model:
            policy_save_path = osp.join(load_model_path, "saved_agent.pb")
        else:
            if agent_type not in ["DDPG", "DDPG_BENCH"]:
                agent.reset_epsilon()
            agent.save(policy_save_path)
            policy_save_path = osp.join(result_dir, "saved_agent.pb")
        for timestep in range(exploitation_steps):
            simulation_results = env.simulate(
                agent_name=agent_name,
                model_path=policy_save_path,
                agent_max_vector_depth=agent_max_vector_depth,
                agent_additional_actions=agent_additional_actions,
                validation_mode=False,
                load_mode=load_model,
                load_path=result_dir,
            )
            zi_payoff = zi_payoff_parser("background", simulation_results)
            tfrl_payoff = zi_payoff_parser("rl_agent", simulation_results)
            writer.add_scalar("agent/exploitation/background_payoff", zi_payoff, timestep)
            writer.add_scalar("agent/exploitation/tfrl", tfrl_payoff, timestep)

    # Clean-up any active resources.
    agent.save(f"{result_dir}/saved_agent/")
    writer.close()
