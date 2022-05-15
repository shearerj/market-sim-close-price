""" A simple table based Q-learning agent. --> This is for python, not for market-sim"""
from __future__ import absolute_import, division, print_function

from dataclasses import dataclass

import dm_env
import gin
import jax
import numpy as np
from bsuite.baselines import base
from bsuite.baselines.utils import replay
from dm_env import specs
from jax import numpy as jnp
import market_manipulation.utils.schedules as schedules

# import egta.utils.schedules as schedules --> Should this be whats written above?
# double check that they are the same

@gin.configurable
@dataclass
class TabularQ(base.Agent):
    """ A simple table based Q-learning agent.

    Args:
        observation_spec:
        action_spec:
        batch_size:
        discount:
        replay_capacity:
        min_replay_size:
        update_period:
        target_update_period:
        learning_rate:
        seed: random seed.
    """

    observation_spec: specs.DiscreteArray
    action_spec: specs.DiscreteArray

    batch_size: int
    discount: float
    replay_capacity: int
    min_replay_size: int
    update_period: int
    learning_rate: float
    exploration_schedule: schedules.Schedule
    seed: int = None

    def __post_init__(self):
        """ Initialize a new DQN agent. """
        self.rng = np.random.RandomState(self.seed)

        self.total_steps = 0
        self.replay = replay.Replay(capacity=self.replay_capacity)
        self.replay_path = None
        self.num_actions = self.action_spec.num_values

        self.q_values = np.zeros([self.observation_spec.num_values, self.action_spec.num_values])

        def train_step(q_values, transitions):
            o_tm1, a_tm1, r_t, d_t, o_t = transitions
            q_tm1 = q_values[o_tm1, a_tm1]
            q_t = jnp.max(q_values[o_t], axis=-1)
            q_target = r_t + d_t * self.discount * q_t - q_tm1
            q_values = jax.ops.index_add(q_values, jax.ops.index[o_tm1, a_tm1], self.learning_rate * q_target)
            return q_values

        self._train_step = jax.jit(train_step)

    def policy(self, timestep: dm_env.TimeStep) -> base.Action:
        """ A policy takes in a timestep and returns an action.

        Args:
          timestep: current timestep.

        Returns:
          Action for the current timestep.
        """
        # Get current exploration schedule.
        self.epsilon = self.exploration_schedule.value(self.total_steps)  # unable to do in java
        self.total_steps += 1

        # Epsilon-greedy policy.
        if self.rng.rand() < self.epsilon:
            return self.rng.randint(self.num_actions)
        q_values = self.q_values[timestep.observation]
        return int(np.argmax(q_values))

    def q_values(self, timestep: dm_env.TimeStep) -> np.ndarray:
        q_values = self._forward(self.parameters, timestep.observation[None, ...])
        return np.asarray(q_values)

    def update(self, timestep: dm_env.TimeStep, action: base.Action, new_timestep: dm_env.TimeStep) -> None:
        """ Updates the agent given a transition.
        modify epsilon here --> ladder func 1. update # time steps exp by amount of exp given (full episde),
        2. query epsilon (fix total time steps)
        Args:
          timestep:
          action:
          new_timestep:
        """
        log = {"epsilon": self.epsilon}

        # Add this transition to the replay buffer.
        self.replay.add(
            [timestep.observation, action, new_timestep.reward, new_timestep.discount, new_timestep.observation]
        )

        if self.total_steps % self.update_period != 0:
            return {}
        if self.replay.size < self.min_replay_size:
            return {}

        # Perform a batch update.
        transitions = self.replay.sample(self.batch_size)
        self.q_values = self._train_step(self.q_values, transitions)
        return log
