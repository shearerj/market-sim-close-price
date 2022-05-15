from typing import Dict, Sequence

import gin
import numpy as np


@gin.configurable
class ParseAveragePayoff:
    """Simple parsing of a Market-Sim log into ZI agent payoffs.

    Assumes:
        - RL observations are just a vector of all of the information available.
        - Action taken is fully specified through price.
    """

    def __call__(self, agent_name_zi: str, log: Dict) -> Sequence:
        """Parse ZI experiences from a log to calculate average payoff.

        Args:
            agent_name_zi:  Name of the agent that tfrl is competing against.
            log: Market-Sim log in JSON format.

        Returns:
            List of average payoffs from ZI agents .
        """
        # Get the player data from the log.
        payoffs = []
        for player_log in log["players"]:
            if player_log["role"] == agent_name_zi:
                payoffs.append(player_log["payoff"])
        # print("avg_payoff", np.mean(np.array(payoffs)))
        return np.mean(np.array(payoffs))

