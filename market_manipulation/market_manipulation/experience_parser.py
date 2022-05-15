""" Parse Market-Sim log into experiences for reinforcement learning.. """
import copy
from typing import Dict, Sequence

import gin
import numpy as np


def flatten_state_dict(
    agent_type: str, state_dict: Dict, bid_len: int, ask_len: int, trade_len: int, max_vector_len: int
) -> np.array:
    """Flatten a state dictionary into an array.
    Args:
        state_dict: Dictionary (JSON) representation of the state.
    Returns:
        Array representation of the state.
    """

    if agent_type == "TabQ":
        state = np.array(
            [
                state_dict["finalFundamentalEstimate"],
                state_dict["privateBid"],
                state_dict["privateAsk"],
                state_dict["omegaRatioBid"],
                state_dict["omegaRatioAsk"],
                state_dict["side"],
                state_dict["bidSize"],
                state_dict["askSize"],
                state_dict["spread"],
                state_dict["marketHoldings"],
                state_dict["contractHoldings"],
                state_dict["numTransactions"],
                state_dict["timeTilEnd"],
                state_dict["latency"],
                state_dict["timeSinceLastTrade"],
            ],
            dtype=float,
        )
    elif agent_type == "DQN" or agent_type == "DDPG":
        # Gets rid of latency and timeSinceLastTrade
        state = np.array(
            [
                state_dict["finalFundamentalEstimate"],
                state_dict["privateBid"],
                state_dict["privateAsk"],
                state_dict["omegaRatioBid"],
                state_dict["omegaRatioAsk"],
                state_dict["side"],
                state_dict["bidSize"],
                state_dict["askSize"],
                state_dict["spread"],
                state_dict["marketHoldings"],
                # state_dict["contractHoldings"],
                # state_dict["numTransactions"],
                state_dict["timeTilEnd"],
                # state_dict["latency"]
                state_dict["timeSinceLastTrade"],
            ],
            dtype=float,
        )
    elif agent_type == "DQN_BENCH" or agent_type == "DDPG_BENCH":
        # Gets rid of latency and timeSinceLastTrade
        state = np.array(
            [
                state_dict["finalFundamentalEstimate"],
                state_dict["privateBid"],
                state_dict["privateAsk"],
                state_dict["omegaRatioBid"],
                state_dict["omegaRatioAsk"],
                state_dict["side"],
                state_dict["bidSize"],
                state_dict["askSize"],
                state_dict["spread"],
                state_dict["marketHoldings"],
                state_dict["contractHoldings"],
                state_dict["numTransactions"],
                state_dict["timeTilEnd"],
                # state_dict["latency"]
                state_dict["timeSinceLastTrade"],
            ],
            dtype=float,
        )
    else:
        assert False, "This is not a valid agent type"

    state = np.concatenate(
        [
            state,
            state_dict["bidVector"][max_vector_len - bid_len : max_vector_len],
            state_dict["askVector"][:ask_len],
            state_dict["transactionHistory"][:trade_len],
        ]
    )
    return state


@gin.configurable
class SimpleExperienceParser:
    """Simple parsing of a Market-Sim log into agent experiences.
    Assumes:
        - RL observations are just a vector of all of the information available.
        - Action taken is fully specified through price.
    """

    def __init__(self):
        super(SimpleExperienceParser, self).__init__()

    def __call__(
        self,
        agent_type: str,
        agent_name: str,
        log: Dict,
        debug_mode: bool,
        reward_shape: bool,
        bid_len: int,
        ask_len: int,
        trade_len: int,
        max_vector_len: int,
    ) -> Sequence:
        """Parse RL experiences from a log.
        Args:
            agent_type: Type of TFRL agent that is being used (TabQ vs DQN)
            agent_name: Name of the agent whose experiences we should load.
            log: Market-Sim log in JSON format.
        Returns:
            List of experiences of form (state0, action, state1, reward, terminal).
        """
        # Get the player data from the log.
        for player_log in log["players"]:
            if player_log["role"] == agent_name:
                break
        assert player_log["role"] == agent_name, f"Could not find player {agent_name}."

        # Parse the observations into experience tuples.
        parsed_experiences = []
        actions = []

        zi_configs = [
            [0, 450, 0.5],
            [0, 600, 0.5],
            [90, 110, 0.5],
            [140, 160, 0.5],
            [190, 210, 0.5],
            [280, 320, 0.5],
            [380, 420, 0.5],
            [380, 420, 1],
            [460, 540, 0.5],
            [950, 1050, 0.5],
        ]

        zim_configs = [
            [380, 420, 0.5, 0],
            [380, 420, 0.5, 250],
            [380, 420, 0.5, 500],
            [380, 420, 0.5, 750],
            [380, 420, 0.5, 1000],
            [380, 420, 0.5, 1250],
            [380, 420, 1.0, 0],
            [380, 420, 1.0, 250],
            [380, 420, 1.0, 500],
            [380, 420, 1.0, 750],
            [380, 420, 1.0, 1000],
            [380, 420, 1.0, 1250],
        ]
        # Grab the payoff from market sim json
        payoff = player_log["payoff"]
        for logged_experience in player_log["features"]["rl_observations"]:
            s0 = flatten_state_dict(
                agent_type,
                logged_experience["state0Dict"],
                bid_len,
                ask_len,
                trade_len,
                max_vector_len,
            )
            # Price isnt real action for TabQ and DQN
            if agent_type == "DDPG" or agent_type == "DDPG_BENCH":
                a = logged_experience["action"]["policyAct"]
            elif agent_type == "DQN_BENCH":
                rmin = logged_experience["action"]["rmin"]
                rmax = logged_experience["action"]["rmax"]
                thresh = logged_experience["action"]["thresh"]
                bench_impact = logged_experience["action"]["benchmarkImpact"]
                a = zim_configs.index([rmin, rmax, thresh, bench_impact])
            else:
                rmin = logged_experience["action"]["rmin"]
                rmax = logged_experience["action"]["rmax"]
                thresh = logged_experience["action"]["thresh"]
                a = zi_configs.index([rmin, rmax, thresh])

            actions += [a]

            s1 = flatten_state_dict(
                agent_type,
                logged_experience["state1Dict"],
                bid_len,
                ask_len,
                trade_len,
                max_vector_len,
            )
            r = logged_experience["reward"]
            if reward_shape:
                r_new = copy.deepcopy(r)
                r = payoff
                payoff -= r_new
            d = logged_experience["terminal"]

            if debug_mode and (agent_type == "DQN" or agent_type == "DQN_BENCH"):
                qval = logged_experience["action"]["qval"]
                parsed_experiences += [(s0, a, s1, r, d, qval)]
            else:
                parsed_experiences += [(s0, a, s1, r, d)]

        return parsed_experiences, player_log["payoff"], actions
