"""Double Deep Q Learning Agent configured for Benchmark Manipulation.

References:
    - https://arxiv.org/abs/1509.06461
"""

import typing

import gin
import sonnet as snt
import tensorflow as tf
from bsuite.baselines.utils import replay
from tensorflow.python.ops import clip_ops

import market_manipulation.utils.schedules as schedules
from market_manipulation.agents.tensorflow_agent import (
    BASE_POLICY_INPUT_TENSOR_SPEC, TensorFlowAgent)
from market_manipulation.agents.zim_agent import ZIMAgent


@gin.configurable
class DQNBenchAgent(TensorFlowAgent):
    """Double Deep Q Learning Agent."""

    def __init__(
        self,
        network: snt.Module,
        network_target: snt.Module,
        optimizer: snt.Optimizer,
        debug_mode: bool,
        reward_shape: bool,
        print_mode: bool,
        reward_clip: bool,
        reward_clip_value: int,
        error_clip: bool,
        error_clip_value: int,
        polyak: bool,
        polyak_tau: int,
        batch_size: int,
        replay_capacity: int,
        min_replay_size: int,
        target_update_period: int,
        num_grad_steps_per_update: int,
        gradient_clip: float,
        process_means: list,
        process_vars: list,
        exploration_schedule: schedules.Schedule,
    ):
        """Constructor.

        Initialize all algorithm specific parameters and modules.

        Args:
            network:
            optimizer: Optimization method (e.g., SGD, ADAM)
            debug_mode: Flag to log debugging information (look at training protocol)
            reward_shape: Flag to perform RL reward shaping technique with final payoff
            print_mode: Flag to print values to the terminal
            reward_clip: Flag to clip rewards in update function
            reward_clip_value: Value to clip rewards to
            error_clip: Flag to clip bellman error in update function
            error_clip_value: Value to clip bellman error to
            polyak: Flag to introduce polyak updates to target network
            polyak_tau: Smoothing value for polyak update
            max_vector_len: Value that represents the maximum length of the bid, ask, and transaction vectors
            bid_len: Value that represents how many values of bid vector to consider
            ask_len: Value that represents how many values of ask vector to consider
            trade_len: Value that represents how many values of transaction history to consider
            batch_size: Batch size for a single training step.
            replay_capacity: Maximum number of experiences contained in replay buffer (FIFO queue).
            min_replay_size: Minimum number of experiences required in replay buffer before training begins.
            target_update_period: How frequently (measured in training updates) to sync the target network's
                parameters.
            num_grad_steps_per_update: Number of gradient steps to take per update
            gradient_clip: Ratio to normalize gradient to
            process_means: List of means used to standardize inputs
            process_vars: List of variances used to standardize inputs
            exploration_schedule: Exploration schedule, which sets the epsilon greediness of the agent
                over the course of training.
        """
        super(DQNBenchAgent, self).__init__()
        # Algorithm specific parameters.
        self.optimizer = optimizer
        self.batch_size = batch_size
        self.debug_mode = debug_mode
        self.reward_shape = reward_shape
        self.print_mode = print_mode
        self.reward_clip = reward_clip
        self.reward_clip_value = reward_clip_value
        self.error_clip = error_clip
        self.error_clip_value = error_clip_value
        self.polyak = polyak
        self.polyak_tau = polyak_tau
        self.min_replay_size = min_replay_size
        self.target_update_period = target_update_period
        self.exploration_schedule = exploration_schedule
        self.replay_buffer = replay.Replay(capacity=replay_capacity)
        self.gradient_clip = gradient_clip
        self.multiplier = 1
        self.num_grad_steps_per_update = num_grad_steps_per_update

        self.process_means = tf.constant(process_means, dtype=tf.float64)
        self.process_vars = tf.constant(process_vars, dtype=tf.float64)

        self.online_network = network
        self.target_network = network_target

        self.total_steps = 0
        self.epsilon = tf.Variable(self.exploration_schedule.value(self.total_steps))

        self.zim_configs = tf.constant(
            [
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
        )

    def set_market_vec_len(self, bid_len, ask_len, trade_len, max_vector_len):
        self.bid_len = bid_len
        self.ask_len = ask_len
        self.trade_len = trade_len
        self.max_vector_len = max_vector_len

    @tf.function(input_signature=BASE_POLICY_INPUT_TENSOR_SPEC)
    def _policy(
        self,
        # Universal inputs
        is_training: bool,
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
        transaction_history: typing.List[float],
    ):
        """TensorFlow implementation of policy."""

        state_pre = tf.stack(
            [
                final_fundamental_estimate,
                private_bid,
                private_ask,
                omega_ratio_bid,
                omega_ratio_ask,
                tf.cast(side, dtype=tf.float64),
                tf.cast(bid_size, dtype=tf.float64),
                tf.cast(ask_size, dtype=tf.float64),
                tf.cast(spread, dtype=tf.float64),
                tf.cast(market_holdings, dtype=tf.float64),
                tf.cast(contract_holdings, dtype=tf.float64),
                tf.cast(num_transactions, dtype=tf.float64),
                tf.cast(time_til_end, dtype=tf.float64),
                tf.cast(time_since_last_trade, dtype=tf.float64),
            ],
            axis=0,
        )

        # Truncate the extra padded values on the bid, ask, and transaction vectors
        bid_vector = tf.slice(
            bid_vector, tf.constant([self.max_vector_len - self.bid_len]), tf.constant([self.bid_len])
        )

        ask_vector = tf.slice(ask_vector, tf.constant([0]), tf.constant([self.ask_len]))
        transaction_history = tf.slice(transaction_history, tf.constant([0]), tf.constant([self.trade_len]))

        state = tf.expand_dims(tf.concat([state_pre, bid_vector, ask_vector, transaction_history], axis=0), axis=0)

        state_post = self.process_input(state)

        network_output = self.online_network(state_post)

        network_choice = tf.argmax(network_output, axis=1)[0]

        # Exploration
        uniform_rv = tf.random.uniform(shape=[], minval=0, maxval=1, dtype=tf.float32)

        explore_cond = uniform_rv < self.epsilon
        zim_id_explore = tf.random.uniform(
            shape=(),
            minval=0,
            maxval=10,
            dtype=tf.int64,
        )
        zim_id = tf.where(explore_cond, x=zim_id_explore, y=network_choice)

        params_chosen = tf.gather(self.zim_configs, zim_id)

        # Sample ZI agent for final action
        zim_chosen = ZIMAgent(
            Rmin=tf.cast(params_chosen[0], dtype=tf.float64),
            Rmax=tf.cast(params_chosen[1], dtype=tf.float64),
            threshold=tf.cast(params_chosen[2], dtype=tf.float64),
            benchmarkImpact=tf.cast(params_chosen[3], dtype=tf.float64),
            ordersPerSide=tf.constant(1, dtype=tf.int32),
            maxPosition=tf.constant(20, dtype=tf.int32),
        )

        price, side, size, rmin, rmax, thresh, bench_impact = zim_chosen.policy(
            is_training,
            final_fundamental_estimate,
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
            transaction_history,
        )
        if self.print_mode:
            tf.print("state after stacking", state_pre)
            tf.print("shape of state after stacking", tf.shape(state_pre))
            tf.print("sliced bid vector", bid_vector)
            tf.print("sliced ask vector", ask_vector)
            tf.print("sliced transaction history", transaction_history)
            tf.print("state after the truncated vectors are added", state)
            tf.print("shape of the state tensor", tf.shape(state))
            tf.print("state after it has been preprocessed", state_post)
            tf.print("output of the online network", network_output)
            tf.print("shape of the online network output", tf.shape(network_output))
            tf.print("maximum output of the online network", tf.reduce_max(network_output))
            tf.print("network choice", network_choice)
            tf.print("epsilon value", self.epsilon)
            tf.print("explored?", explore_cond)
            tf.print("params chosen", params_chosen)
            tf.print("price to submit", price)
        return (
            price,
            side,
            size,
            tf.cast(rmin, dtype=tf.float32),
            tf.cast(rmax, dtype=tf.float32),
            tf.cast(thresh, dtype=tf.float32),
            tf.cast(bench_impact, dtype=tf.float32),
            tf.reduce_max(network_output),
        )

    def _training_step(self, observation_tm1, zi_id, observation_t, reward_t, done_t):
        """Perform a single training update of our policy's parameters.

        Args:
            transitions: (s, a, zi, s'. r', d') tuple of experiences sampled from the replay buffer to
                use for this training step.

        Returns:
            loss dictionary.
        """
        with tf.GradientTape() as tape:
            # Q-Value predictions, given state.
            q_values_tm1 = self.online_network(observation_tm1)
            q_values_t = tf.stop_gradient(self.target_network(observation_t))

            # Get Q-Values given state and action.
            actions_tm1 = tf.one_hot(zi_id, tf.shape(q_values_t)[1])
            q_action_tm1 = tf.reduce_sum(q_values_tm1 * actions_tm1, axis=-1)
            q_action = tf.reduce_max(q_values_t, axis=-1)

            # One-step Q-learning loss.
            if self.reward_clip:
                reward_t = tf.clip_by_value(
                    reward_t, clip_value_min=-self.reward_clip_value, clip_value_max=self.reward_clip_value
                )
            if self.reward_shape:
                target = reward_t
            else:
                target = reward_t + 0.99 * tf.cast(q_action, dtype=tf.float64)

            td_error = tf.cast(q_action_tm1, dtype=tf.float64) - target
            if self.error_clip:
                td_error = tf.clip_by_value(
                    td_error, clip_value_min=-self.error_clip_value, clip_value_max=self.error_clip_value
                )
            loss = 0.5 * tf.reduce_mean(tf.pow(td_error, 2))

        # Update the online network via SGD.
        variables = self.online_network.trainable_variables
        gradients = tape.gradient(loss, variables)

        clipped_gradients, norm = clip_ops.clip_by_global_norm(gradients, self.gradient_clip)
        self.optimizer.apply(clipped_gradients, variables)
        if self.print_mode:
            tf.print("q values for state 0", q_values_tm1)
            tf.print("q values for state 1", q_values_t)
            tf.print("Shape of the q values", tf.shape(q_values_t))
            tf.print("ZI chosen", zi_id)
            tf.print("one hot encoding for ZI agent chosen here", actions_tm1)
            tf.print("Q(state, action)", q_action_tm1)
            tf.print("max over a of Q(s',a)", q_values_t)
            tf.print("reward vector", reward_t)
            tf.print("maximum reward", max(reward_t))
            tf.print("minimum reward", min(reward_t))
            tf.print("yi = reward + gamma*max over a of Q(s',a')", target)
            tf.print("TD error", td_error)
            tf.print("MSE loss", loss)
            tf.print("norm", norm)
        return {"loss": loss.numpy(), "gradient-norm": norm.numpy()}

    def update(self) -> typing.Dict:
        """Perform a training update on the agent.

        Returns:
            Dictionary of detailed statistics from this training update. The following fields should be expected:
                - epsilon: (double) the epsilon-greediness of the policy at the time of the update.
                - loss: (double) loss calculated from this sampled batch of experience.
                - gradient norm: (double) norm of the gradient step that is calculated by tensorflow
        """
        log = {"epsilon": self.epsilon.numpy()}

        if self.replay_buffer.size < self.min_replay_size:
            return log

        # Perform self.num_grad_steps_per_update # of training steps.
        for i in range(self.num_grad_steps_per_update):
            transitions = self.replay_buffer.sample(self.batch_size)
            if self.debug_mode:
                s, a, s_prime, r_prime, d_prime, qval = transitions
            else:
                s, a, s_prime, r_prime, d_prime = transitions
            s = tf.constant(s)
            a = tf.constant(a, dtype=tf.int32)
            r_prime = tf.constant(r_prime, dtype=tf.float64)
            d_prime = tf.constant(d_prime, dtype=tf.float64)
            s_prime = tf.constant(s_prime)
            d_prime = tf.where(
                d_prime == tf.constant(0, dtype=tf.float64),
                x=tf.constant(1, dtype=tf.float64),
                y=tf.constant(0, dtype=tf.float64),
            )

            s = self.process_input(s)
            s_prime = self.process_input(s_prime)
            update_log = self._training_step(s, a, s_prime, r_prime, d_prime)

        log.update(update_log)

        # Periodically update target network variables.
        # Total steps increases randomly by ~25, so setting 30 will cause a 1 update delay
        if self.total_steps > (self.multiplier * self.target_update_period):
            self.multiplier += 1
            payload = zip(self.target_network.trainable_variables, self.online_network.trainable_variables)
            if self.polyak:
                for target, param in payload:
                    target.assign(self.polyak_tau * target + (1 - self.polyak_tau) * param)
            else:
                for target, param in payload:
                    target.assign(param)

        if self.print_mode:
            tf.print("shape of the s0 batch", s.shape)
            tf.print("processed batch states", s)
            tf.print("processed batch next states", s_prime)

        return log

    def record_experiences(self, experiences) -> None:
        """Record a sequence of experiences into the agent's replay buffer.

        Args:
            experiences: List of experience tuples.
        """
        self.total_steps += len(experiences)
        if self.print_mode:
            print("# steps seen", self.total_steps)
        self.epsilon.assign(self.exploration_schedule.value(self.total_steps))

        for experience in experiences:
            self.replay_buffer.add(experience)

    def process_input(self, input_state):
        """Process the state input for passing into neural network based on expert values

        Args:
            input_state: (batch, x) input tensor

        """
        return tf.math.divide(input_state - self.process_means, tf.math.sqrt(self.process_vars))

    def reset_epsilon(self):
        self.epsilon.assign(0)

    def set_epsilon(self):
        self.epsilon.assign(self.exploration_schedule.value(self.total_steps))


@gin.configurable
class QNetworkBench(snt.Module):
    """Example Neural Network Module in TensorFlow."""

    def __init__(self, name=None):
        """Constructor.

        You should create all modules that have variables you want to train here. How the modules interact
        and all other mathematical operations on the preexisting variables can occur in the call.

        Args:
            name: Module name for scoping variables. e.g.,
                ```python
                nn1 = QNetwork(name="A")  # All member variables will be saved under name "A/{name}".
                nn2 = QNetwork(name="B")  # All member variables will be saved under name "B/{name}".
                ```
        """
        super(QNetworkBench, self).__init__(name=name)
        self.layers = tf.keras.Sequential(
            [
                tf.keras.layers.InputLayer(input_shape=(29,)),  # Still expects a batch dimension
                tf.keras.layers.Dense(26, activation="relu", name="hidden1"),
                tf.keras.layers.Dense(26, activation="relu", name="hidden2"),
                tf.keras.layers.Dense(26, activation="relu", name="hidden3"),
                tf.keras.layers.Dense(10),
            ]
        )

    def __call__(self, observation):
        """Run the module.

        This interface can be changed as needed.

        Args:
            observation:
        """
        output = self.layers(observation)
        return output
