""" Basic agent API for Market-Sim.

This follows very closely with the `egta.rl` agent design; however, because Market-Sim
does not conform to the dm-env API, it must be distinct.

NOTE: we're separating the definition of a policy from an agent. This is useful because
sometimes we may want to build all sorts of fancy policies that are all trained using
the same mechanisms (that the agent will own).
"""
import typing

import gin


class BaseAgent:
    """Market-Sim agent interface."""

    def policy(self, observation) -> float:
        """Run inference the agent's policy.

        Args:
            observation: Observation of the market.

        Returns:
            Action selected.
        """
        raise NotImplementedError()

    def record_experiences(self, experiences) -> None:
        """Record a sequence of experiences into the agent's replay buffer.

        Args:
            experiences: List of experience tuples.
        """
        pass

    def update(self) -> typing.Dict:
        """Perform a training update on the agent."""
        return {}

    def save(self, path) -> None:
        """Save agent to disk.

        Args:
            path: Path to new directory to save agent to.
        """
        pass


@gin.configurable
class DummyAgent(BaseAgent):
    def policy(self, observation) -> float:
        pass
