from __future__ import absolute_import, division, print_function
import market_manipulation.agents.tensorflow_agent as tfa
from market_manipulation.agents.zi_agent import ZIAgent
import typing
import tensorflow as tf
# import dm_env
import gin
from bsuite.baselines.utils import replay
# from dm_env import specs
import market_manipulation.utils.schedules as schedules
from market_manipulation.feature_processing.tiling import create_features, encode


@gin.configurable
class TabularQ(tfa.TensorFlowAgent):
    """Market-Sim agent interface."""

    def __init__(
            self,
            batch_size: int,
            discount: float,
            replay_capacity: int,
            min_replay_size: int,
            learning_rate: float,
            exploration_schedule: schedules.Schedule,
    ):
        """Constructor.
        A simple table based Q-learning agent.

        Args:
            batch_size:
            discount:
            replay_capacity:
            min_replay_size:
            learning_rate:
            exploration_schedule

    """
        super().__init__()
        self.batch_size = batch_size
        self.discount = discount
        self.replay_capacity = replay_capacity
        self.min_replay_size = min_replay_size
        self.learning_rate = learning_rate
        self.exploration_schedule = exploration_schedule
        self.replay = replay.Replay(capacity=self.replay_capacity)  # Initialize a replay buffer w/given capacity
        # Actions that will be taken are chosing ZI agent parameters based on paper
        self.zi_configs = [
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
        self.num_actions = len(self.zi_configs)
        # Initialize empty q table with size = (bin_combinations, num_actions)
        self.q_table = tf.Variable(tf.cast(tf.fill([81, 10], 0), dtype=tf.float64))
        # create 10 reusable ZI agents
        # self.zi_agents = []  # Tensor array if this gives issues?
        self.vector_depth = 20
        self.total_steps = 0
        self.epsilon = self.exploration_schedule.value(self.total_steps)
        self.loss = 0

    @tf.function(input_signature=tfa.BASE_POLICY_INPUT_TENSOR_SPEC)
    def _policy(
            self,
            # Universal Input
            final_fundamental_estimate: float,
            private_bid: float,
            private_ask: float,
            omega_ratio_bid: float,
            omega_ratio_ask: float,
            side: int,
            bid_size: int,
            ask_size: int,
            spread: int,
            market_holdings: int,
            contract_holdings: float,
            num_transactions: int,
            time_til_end: int,
            latency: int,
            time_since_last_trade: int,
            bid_vector: typing.List[float],
            ask_vector: typing.List[float],
            transaction_history: typing.List[float]
    ):
        """Policy that returns an action based on the state dictionary values (equivalent to current time-step).
        Returns:
            Action selected.
        """
        # Process the inputs using feature processing functions
        povd_feature = create_features(final_fundamental_estimate, private_bid, private_ask, omega_ratio_bid,
                                       omega_ratio_ask, side, bid_size, ask_size, time_since_last_trade,
                                       bid_vector, ask_vector)
        bin_value = encode(povd_feature)
        # tf.print(tf.math.reduce_sum(self.q_table))
        state_slice = tf.gather(self.q_table, indices=bin_value)
        zi_id_exploit = tf.argmax(state_slice, output_type=tf.int32)  # Should return between 0-9
        # tf.print(tf.reduce_max(state_slice))

        # Exploration
        uniform_rv = tf.random.uniform(shape=[], minval=0, maxval=1, dtype=tf.float32)
        explore_cond = uniform_rv < self.epsilon
        zi_id_explore = tf.random.uniform(
                shape=(),
                minval=0,
                maxval=self.num_actions,  # Non-inclusive
                dtype=tf.int32,
            )
        zi_id = tf.where(explore_cond, x=zi_id_explore, y=zi_id_exploit)
        tf.print(tf.reduce_max(self.q_table))
        tf.print(tf.reduce_min(self.q_table))
        if zi_id != 0:
            tf.print("hello")
            tf.print(zi_id)
        else:
            tf.print("bye")
            tf.print(bin_value)
            tf.print(state_slice)
        # Pick action from given id
        # try out random zi
        # zi_id = tf.random.uniform(shape=[], minval=0, maxval=9, dtype=tf.int32)
        # tf.print(zi_id)
        params_chosen = tf.gather(self.zi_configs, zi_id)  # Should be a (3, ) tensor with the configuration values
        zi_chosen = ZIAgent(Rmin=tf.cast(params_chosen[0], dtype=tf.float64),
                            Rmax=tf.cast(params_chosen[1], dtype=tf.float64),
                            threshold=tf.cast(params_chosen[2], dtype=tf.float64),
                            ordersPerSide=tf.constant(1, dtype=tf.int32),
                            maxPosition=tf.constant(20, dtype=tf.int32)
                            )
        price, side, size, rmin, rmax, thresh = zi_chosen.policy(final_fundamental_estimate,
                                                                 private_bid,
                                                                 private_ask,
                                                                 omega_ratio_bid,
                                                                 omega_ratio_ask,
                                                                 side,
                                                                 bid_size,
                                                                 ask_size,
                                                                 spread,
                                                                 market_holdings,
                                                                 contract_holdings,
                                                                 num_transactions,
                                                                 time_til_end,
                                                                 latency,
                                                                 time_since_last_trade,
                                                                 bid_vector,
                                                                 ask_vector,
                                                                 transaction_history
                                                                 )
        return price, side, size, tf.cast(rmin, dtype=tf.float32), tf.cast(rmax, dtype=tf.float32), tf.cast(thresh, dtype=tf.float32)

    def record_experiences(self, experiences) -> None:
        """Record a sequence of experiences into the agent's replay buffer.
        Args:
            experiences: List of experience tuples.
        """
        self.total_steps += len(experiences)
        self.epsilon = self.exploration_schedule.value(self.total_steps)
        for exp in experiences:
            # Structure is [(s0,a,zi_id, s1,r1,d1), ...] where each value inside tuple unpacks into appropriate size
            self.replay.add(exp)

    def update(self) -> dict:
        """Perform a training update on the agent."""
        log = {"epsilon": self.epsilon, "loss": self.loss}
        if self.replay.size < self.min_replay_size:  # When # of items added isnt enough to get batches
            return log, self.q_table

        # Sample from replay buffer
        transitions = self.replay.sample(self.batch_size)
        s, a, zi_id, s_prime, r_prime, d_prime = transitions  # Unpack the items from the returned [B, ...]

        # Cast values as tensors
        s = tf.constant(s)
        a = tf.constant(a, dtype=tf.float64)  # Not necessary for TabQ, will need for DQN
        zi_id = tf.constant(zi_id, dtype=tf.int32)
        r_prime = tf.constant(r_prime, dtype=tf.float64)
        d_prime = tf.constant(d_prime, dtype=tf.float64)  # Assuming this needs to be float for multiplication
        s_prime = tf.constant(s_prime)

        # Get the bin values for the state0 and state1 values
        povd_feature_state = create_features(s[:, 0], s[:, 1], s[:, 2], s[:, 3], s[:, 4], s[:, 5], s[:, 6], s[:, 7],
                                             s[:, 14], s[:, 15:15+self.vector_depth],
                                             s[:, 15+self.vector_depth:15+2*self.vector_depth])
        bin_value_state = encode(povd_feature_state)

        povd_feature_prime = create_features(s_prime[:, 0], s_prime[:, 1], s_prime[:, 2], s_prime[:, 3],
                                             s_prime[:, 4], s_prime[:, 5], s_prime[:, 6], s_prime[:, 7],
                                             s_prime[:, 14], s_prime[:, 15:15+self.vector_depth],
                                             s_prime[:, 15+self.vector_depth:15+2*self.vector_depth])
        bin_value_prime = encode(povd_feature_prime)

        new_q_table, loss = self._training_step(bin_value_state, zi_id, bin_value_prime, r_prime, d_prime)
        # tf.print(self.q_table)
        log["loss"] = loss.numpy()
        # tf.print(type(log["loss"]))
        return log, new_q_table.numpy()

    def _training_step(self, state_index, action, state_index_prime, reward, done):
        """ All inputs should be tensors of size (batch, )
            Args:
                state_index: tensor of Q-table indices corresponding to states
                action: tensor of actions.
                state_index_prime: tensor of successor state indices.
                reward: tensor of rewards
                done: tensor determining whether we are done
        """
        # Cast the discount and learning rate to be compatible tensors for multiplication
        discount = tf.cast(self.discount, tf.float64)
        lr = tf.cast(self.learning_rate, tf.float64)

        # Create the indices (state,action) from each batch and grab those values from qtable
        current_state_action_indices = tf.stack([state_index, action], axis=1)  # each row is a (state, action) index
        current_state_q_values = tf.cast(tf.gather_nd(self.q_table, indices=current_state_action_indices),
                                         dtype=tf.float64)  # Gets each (state, action) index and turns into vector
        tf.print(current_state_action_indices)
        # Grab the next state row for each batch and find the maximum for Bellman update
        next_state_q_values = tf.cast(tf.gather(self.q_table, indices=state_index_prime), dtype=tf.float64)
        next_state_action_q_values = tf.reduce_max(next_state_q_values, axis=1)

        # Calculate target for temporal difference update
        # Done = 0 when NOT terminal, Done = 1 when terminal according to JSON
        done_val = tf.where(done == tf.constant(0, dtype=tf.float64), x=tf.constant(1, dtype=tf.float64),
                            y=tf.constant(0, dtype=tf.float64))
        q_error = reward + done_val * discount * next_state_action_q_values - current_state_q_values  # Bellman
        q_error = lr * q_error
        q_update = current_state_q_values + q_error

        # Update the q table (returns an update q table, doesnt directly change values)
        self.q_table = tf.tensor_scatter_nd_update(
            self.q_table,  # Tensor
            current_state_action_indices,  # Indices
            q_update  # Updates
        )

        # Calculate the MSE loss between new state q value and current state q value
        loss = tf.math.reduce_mean(tf.math.square(q_update - current_state_q_values))
        return self.q_table, loss
