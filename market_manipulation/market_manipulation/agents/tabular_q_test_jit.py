""" Tests for egta.rl.tabular_q.TabularQ. """
from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import numpy as np
from absl.testing import absltest
from dm_env import specs

from egta.rl.tabular_q import TabularQ
from egta.utils.schedules import ConstantSchedule


class TabularQTest(absltest.TestCase):

    def test_update(self):
        agent = TabularQ(
            observation_spec=specs.DiscreteArray(dtype=int, num_values=2),
            action_spec=specs.DiscreteArray(dtype=int, num_values=2),
            discount=0.99,
            learning_rate=0.003,
            # Not important for this test case.
            batch_size=32,
            replay_capacity=10000,
            min_replay_size=100,
            update_period=1,
            seed=1,
            exploration_schedule=ConstantSchedule(x=0.03))   

        # 1. The first q update we check has zeros for the initial
        #    q values. This reduces computation to just r*alpha.
        q_values = np.zeros_like(agent.q_values)

        transitions = (
            np.array([0, 1]),  # o_tm1
            np.array([1, 0]),  # a_tm1
            np.array([1, 2]),  # r_t
            np.array([0, 0]),  # d_t
            np.array([0, 0]))  # o_t
        
        q_values = agent._train_step(q_values, transitions)
        print(q_values)

        np.testing.assert_array_almost_equal(
            q_values,
            np.array([[0, 0.003], [0.006, 0]]))

        # 2. We are going to repeat the same q update, but now 
        #    the q-values will have non-zero values. This will test
        #    the full Bellman equation.
        q_values = agent._train_step(q_values, transitions)
        print(q_values)

        np.testing.assert_array_almost_equal(
            q_values,
            np.array([[0, 0.00599], [0.01199, 0]]),
            decimal=4)


if __name__ == "__main__":
    absltest.main()    
