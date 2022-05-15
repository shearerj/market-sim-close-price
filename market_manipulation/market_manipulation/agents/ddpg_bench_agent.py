"""Deep Determinisic Policy Gradient Learning Agent configured for Benchmark Manipulation.

References:
    - https://arxiv.org/abs/1509.02971
"""
import copy
import typing

import gin
import numpy as np
import sonnet as snt
import tensorflow as tf
from bsuite.baselines.utils import replay
from tensorflow.python.ops import clip_ops

import market_manipulation.utils.schedules as schedules
# from market_manipulation.utils.ou_noise import OrnsteinUhlenbeckActionNoise
from market_manipulation.utils.zscore_normalize_bench import ZScoreNormalizationBench

from .tensorflow_agent import BASE_POLICY_INPUT_TENSOR_SPEC, TensorFlowAgent


@gin.configurable
class DDPGBenchAgent(TensorFlowAgent):
    """Deep Deterministic Policy Gradient Agent."""

    def __init__(
        self,
        critic_network: snt.Module,
        actor_network: snt.Module,
        critic_optimizer: snt.Optimizer,
        actor_optimizer: snt.Optimizer,
        batch_size: int,
        replay_capacity: int,
        min_replay_size: int,
        num_grad_steps_per_update: int,
        target_update_period: int,
        gamma: float,
        tau: float,
        action_coefficient: int,
        benchmark_impact: int,
        benchmark_dir: int,
        num_actions: int,
        ou_sigma: float,
        ou_theta: float,
        ou_dt: float,
        exploration_noise: float,
        noise_scaling: float,
        clipped: float,
        clip_reward: int,
        pop_stats_path: str,
        exploration_schedule: schedules.Schedule,
    ):
        """Constructor.

        Initialize all algorithm specific parameters and modules.

        Args:
            optimizer: Optimization method (e.g., SGD, ADAM)
            batch_size: Batch size for a single training step.
            replay_capacity: Maximum number of experiences contained in replay buffer (FIFO queue).
            min_replay_size: Minimum number of experiences required in replay buffer before training begins.
            target_update_period: How frequently (measured in training updates) to sync the target network's
                parameters.
            exploration_schedule: Exploration schedule, which sets the epsilon greediness of the agent
                over the course of training.
        """
        super(DDPGBenchAgent, self).__init__()
        # Algorithm specific parameters.
        self.critic_optimizer = critic_optimizer
        self.actor_optimizer = actor_optimizer
        self.batch_size = batch_size
        self.min_replay_size = min_replay_size
        self.num_grad_steps_per_update = num_grad_steps_per_update
        self.target_update_period = target_update_period
        self.multiplier = 1
        self.exploration_schedule = exploration_schedule
        self.replay_buffer = replay.Replay(capacity=replay_capacity)
        self.gamma = gamma
        self.tau = tau
        self.action_coefficient = action_coefficient
        self.benchmark_impact = benchmark_impact
        self.benchmark_dir = benchmark_dir
        self.clipped = clipped
        self.min_reward = -1 * clip_reward
        self.max_reward = clip_reward
        self.reward_clipping = True
        self.max_vector_len = 0
        self.bid_len = 0
        self.ask_len = 0
        self.trade_len = 0
        self.noise_scaling = noise_scaling

        self.online_critic = critic_network
        self.target_critic = copy.copy(critic_network)  # TODO: Verify that this copy is safe in TF.

        self.online_actor = actor_network
        self.target_actor = copy.copy(actor_network)  # TODO: Verify that this copy is safe in TF.

        self.epsilon = 0.0
        self.total_steps = 0

        self.buffer_size = tf.Variable(self.replay_buffer.size)

        self.exploration_noise = exploration_noise
        # Initialise Ornstein-Uhlenbeck Noise generator
        # self.exploration_noise = OrnsteinUhlenbeckActionNoise(
        #    mu=np.zeros(
        #        [
        #            num_actions,
        #        ]
        #    ),
        #    sigma=ou_sigma,
        #    theta=ou_theta,
        #    dt=ou_dt,
        # )

        self.zscore_norm = ZScoreNormalizationBench(pop_stats_path)

    def set_reward_clipping(self, reward_clipping):
        self.reward_clipping = reward_clipping

    def set_market_vec_len(self, bid_len, ask_len, trade_len, max_vector_len):
        self.bid_len = bid_len
        self.ask_len = ask_len
        self.trade_len = trade_len
        self.max_vector_len = max_vector_len

    @tf.function(input_signature=BASE_POLICY_INPUT_TENSOR_SPEC)
    def _policy(
        self,
        # Universal inputs.
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
    ) -> (float, int, int):
        """TensorFlow implementation of policy.

        See `experiments.market_sim.agents.TensorFlowAgent.policy` for interface description.
        """
        noise = 0.0
        if self.buffer_size < self.min_replay_size:
            act = tf.random.uniform(shape=[], minval=0.0, maxval=1.0, dtype=tf.float32)
            # rand_act = tf.random.normal(shape=[], mean=0.0, stddev=0.5, dtype=tf.float64)
            # act = tf.abs(rand_act)
            # abs_act = tf.abs(rand_act)
            # if side == 1:
            #    act = tf.math.multiply(abs_act, -1)
            # else:
            #    act = tf.math.multiply(abs_act, 1)
        else:
            obs_x = tf.stack(
                [
                    # If any changes are made to the feature space, also edit in 'utils/zscore_normalize.py'
                    tf.cast(final_fundamental_estimate, tf.float32),
                    tf.cast(private_bid, tf.float32),
                    tf.cast(private_ask, tf.float32),
                    tf.cast(omega_ratio_bid, tf.float32),
                    tf.cast(omega_ratio_ask, tf.float32),
                    tf.cast(side, tf.float32),
                    tf.cast(bid_size, tf.float32),
                    tf.cast(ask_size, tf.float32),
                    tf.cast(spread, tf.float32),
                    tf.cast(market_holdings, tf.float32),
                    tf.cast(contract_holdings, tf.float32),
                    tf.cast(num_transactions, tf.float32),
                    tf.cast(time_til_end, tf.float32),
                    # tf.cast(latency, tf.float32),
                    tf.cast(time_since_last_trade, tf.float32),
                ]
            )
            shape_no_vec = 14
            obs_y = tf.reshape(obs_x, [1, shape_no_vec])
            obs_z = tf.concat(
                [
                    obs_y,
                    tf.cast(
                        tf.reshape(bid_vector[self.max_vector_len - self.bid_len : self.max_vector_len], [1, -1]),
                        tf.float32,
                    ),
                    tf.cast(tf.reshape(ask_vector[: self.ask_len], [1, -1]), tf.float32),
                    tf.cast(tf.reshape(transaction_history[: self.trade_len], [1, -1]), tf.float32),
                ],
                axis=1,
            )
            obs_size = shape_no_vec + self.bid_len + self.ask_len + self.trade_len
            obs_z = self.zscore_norm(
                tf.reshape(obs_z, [1, obs_size]), self.bid_len, self.ask_len, self.trade_len, self.max_vector_len
            )
            # act = tf.cast(self.online_actor(obs_z), dtype.float64)
            act = self.online_actor(obs_z)
            if is_training:
                noise = tf.random.normal(shape=[], mean=0.0, stddev=self.exploration_noise, dtype=tf.float32)
                # noise = self.exploration_noise() * self.noise_scaling
                act += noise

        act = tf.clip_by_value(act, clip_value_min=0, clip_value_max=1)
        # act += tf.random.normal(shape=[], mean=0.0, stddev=0.001, dtype=tf.float64)
        # act = tf.clip_by_value(act, clip_value_min=-1, clip_value_max=1)

        if side == 1:
            private_benefit = private_bid
            # act = tf.clip_by_value(act, clip_value_min=-1, clip_value_max=0)
        else:
            private_benefit = private_ask

        bi1 = self.benchmark_impact * self.benchmark_dir
        bi2 = tf.multiply(tf.cast(side, dtype=tf.float32), bi1)
        bi3 = tf.subtract(tf.cast(self.action_coefficient, dtype=tf.float32), bi2)
        ad1 = tf.multiply(tf.cast(side, dtype=tf.float32), act)
        surplus_demand = tf.multiply(ad1, bi3)
        estimated_value = tf.add(tf.cast(final_fundamental_estimate, tf.float32), tf.cast(private_benefit, tf.float32))
        price = tf.subtract(estimated_value, surplus_demand)
        # price = estimated_value * float('nan')
        size = tf.constant(1, name="size", dtype=tf.int32)
        if is_training:
            self.buffer_size.assign_add(1)

        price = tf.cast(price, dtype=tf.float64)
        act = tf.cast(act, dtype=tf.float32)
        return price, side, size, act

    @tf.function
    def _training_step(self, transitions):
        """Perform a single training update of our policy's parameters.

        Args:
            transitions: (s, a, r', d', s') tuple of experiences sampled from the replay buffer to
                use for this training step.

        Returns:
            Empty dictionary. TODO: Add loss.
        """
        observation_tm1, action_tm1, observation_t, reward_t, done_t = transitions
        observation_t = tf.cast(tf.convert_to_tensor(observation_t), tf.float32)
        observation_tm1 = tf.cast(tf.convert_to_tensor(observation_tm1), tf.float32)
        action_tm1 = tf.cast(action_tm1, tf.float32)
        reward_t = tf.cast(reward_t, tf.float32)
        done_t = tf.cast(done_t, tf.float32)

        with tf.GradientTape() as a_tape, tf.GradientTape() as c_tape:

            target_actions = self.target_actor(observation_t)
            target_next_state_values = tf.squeeze(self.target_critic(observation_t, target_actions), 1)
            critic_value = tf.squeeze(self.online_critic(observation_tm1, tf.expand_dims(action_tm1, axis=-1)), 1)
            target_values = reward_t + self.gamma * target_next_state_values * (1 - done_t)
            critic_loss = tf.keras.losses.MSE(target_values, critic_value)

            # Only update the actor when updating the target networks
            if self.total_steps > (self.multiplier * self.target_update_period):
                new_policy_actions = self.online_actor(observation_tm1)
                actor_loss = -self.online_critic(observation_tm1, new_policy_actions)
                actor_loss = tf.reduce_mean(actor_loss)

        variables_critic = self.online_critic.trainable_variables
        grads_critic = c_tape.gradient(critic_loss, variables_critic)
        clipped_grads_critic, _ = clip_ops.clip_by_global_norm(grads_critic, self.clipped)
        self.critic_optimizer.apply(clipped_grads_critic, variables_critic)

        # Only update the actor when updating the target networks
        if self.total_steps > (self.multiplier * self.target_update_period):
            variables_actor = self.online_actor.trainable_variables
            grads_actor = a_tape.gradient(actor_loss, variables_actor)
            clipped_grads_actor, _ = clip_ops.clip_by_global_norm(grads_actor, self.clipped)
            self.actor_optimizer.apply(clipped_grads_actor, variables_actor)

        # return actor_loss, critic_loss
        if self.total_steps > (self.multiplier * self.target_update_period):
            loss_log = {"actor_loss": actor_loss, "critic_loss": critic_loss}
        else:
            loss_log = {"critic_loss": critic_loss}

        return loss_log

    def update(self) -> typing.Dict:
        """Perform a training update on the agent.

        Returns:
            Dictionary of detailed statistics from this training update. The following fields should be expected:
                - epsilon: (double) the epsilon-greediness of the policy at the time of the update.
                TODO: - loss: (double) loss calculated from this sampled batch of experience.
                TODO: - return: (double) recent average return the agent received.
                TODO:       This will be calculated from the log-parser and will be an average of the last k
                TODO:       episode's total payoffs. Let k be a configurable parameter.
        """
        log = {"epsilon": self.epsilon}

        # Let the agent's replay buffer contain some minimum amount of diversity in experience before
        # beginning training.
        if self.replay_buffer.size < self.min_replay_size:
            return log

        # Perform self.num_grad_steps_per_update # of training steps.
        for i in range(self.num_grad_steps_per_update):
            transitions = self.replay_buffer.sample(self.batch_size)
            transitions[0] = self.zscore_norm(
                transitions[0], self.bid_len, self.ask_len, self.trade_len, self.max_vector_len
            )
            transitions[2] = self.zscore_norm(
                transitions[2], self.bid_len, self.ask_len, self.trade_len, self.max_vector_len
            )
            if self.reward_clipping:
                transitions[3] = np.clip(transitions[3], a_min=self.min_reward, a_max=self.max_reward)
            # actor_loss, critic_loss = self._training_step([tf.constant(x) for x in transitions])
            update_pre_log = self._training_step([tf.constant(x) for x in transitions])

        if self.total_steps <= (self.multiplier * self.target_update_period):
            update_log = {"critic_loss": update_pre_log["critic_loss"].numpy()}
        # Periodically update target network variables.
        else:
            self.multiplier += 1
            update_log = {
                "actor_loss": update_pre_log["actor_loss"].numpy(),
                "critic_loss": update_pre_log["critic_loss"].numpy(),
            }
            # update target critic variables
            payload_critic = zip(self.target_critic.trainable_variables, self.online_critic.trainable_variables)
            for target, param in payload_critic:
                target.assign(param * self.tau + target * (1 - self.tau))
            # update target actor variables
            payload_actor = zip(self.target_actor.trainable_variables, self.online_actor.trainable_variables)
            for target, param in payload_actor:
                target.assign(param * self.tau + target * (1 - self.tau))

        log.update(update_log)

        return log

    def normalizeObs(self, obs):
        obs_arr = np.array(obs)
        obs_norm = np.linalg.norm(obs_arr, axis=0)
        return (obs_arr / obs_norm).tolist()

    def record_experiences(self, experiences) -> None:
        """Record a sequence of experiences into the agent's replay buffer.

        Args:
            experiences: List of experience tuples.
        """
        self.total_steps += len(experiences)
        self.epsilon = self.exploration_schedule.value(self.total_steps)
        self.buffer_size.assign_add(len(experiences))

        for experience in experiences:
            self.replay_buffer.add(experience)
