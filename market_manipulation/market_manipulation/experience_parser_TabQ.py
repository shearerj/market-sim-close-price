""" Parse Market-Sim log into experiences for reinforcement learning.. """
from typing import Dict, Sequence

import gin
import numpy as np


def flatten_state_dict(state_dict: Dict) -> np.array:
    """Flatten a state dictionary into an array.

    Args:
        state_dict: Dictionary (JSON) representation of the state.

    Returns:
        Array representation of the state.
    """
    # See `deeprl_market_sim.agents.tensorflow_agent.BASE_POLICY_INPUT_TENSOR_SPEC` for more details
    # about the state space definition.
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
    state = np.concatenate(
        [
            state,
            state_dict["bidVector"],
            state_dict["askVector"],
            state_dict["transactionHistory"],
        ]
    )
    return state


@gin.configurable
class SimpleExperienceParserTabQ:
    """Simple parsing of a Market-Sim log into agent experiences.

    Assumes:
        - RL observations are just a vector of all of the information available.
        - Action taken is fully specified through price.
    """

    def __call__(self, agent_name: str, log: Dict) -> Sequence:
        """Parse RL experiences from a log.

        Args:
            agent_name:  Name of the agent whose experiences we should load.
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
            [950, 1050, 0.5]
        ]
        for logged_experience in player_log["features"]["rl_observations"]:
            s0 = flatten_state_dict(logged_experience["state0Dict"])
            a = logged_experience["action"]["price"]  # TODO(max): Customize depending on agent type.
            rmin = logged_experience["action"]["rmin"]
            rmax = logged_experience["action"]["rmax"]
            thresh = logged_experience["action"]["thresh"]
            zi_id = zi_configs.index([rmin, rmax, thresh])
            s1 = flatten_state_dict(logged_experience["state1Dict"])
            r = logged_experience["reward"]
            d = logged_experience["terminal"]
            parsed_experiences += [(s0, a, zi_id, s1, r, d)]

        return parsed_experiences
