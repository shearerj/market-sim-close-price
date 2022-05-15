""" A savable Tensorflow agent API for Market-Sim.

"""
import typing

import tensorflow as tf

from market_manipulation.agents.base_agent import BaseAgent

# Define the expected graph inputs.
# These are defined at: https://strategicreasoning.eecs.umich.edu/ebrink/market-sim/blob/latency/market-sim/src/main/java/edu/umich/srg/learning/TensorFlowAction.java  # noqa: E501
# We must do this because TensorFlow in Java requires knowing the parameter names
# in order to interface with a loaded model.
BASE_POLICY_INPUT_TENSOR_SPEC = [
    tf.TensorSpec(name="isTraining", shape=[], dtype=tf.bool),
    tf.TensorSpec(name="finalFundamentalEstimate", shape=[], dtype=tf.float64),
    tf.TensorSpec(name="privateBid", shape=[], dtype=tf.float64),
    tf.TensorSpec(name="privateAsk", shape=[], dtype=tf.float64),
    tf.TensorSpec(name="omegaRatioBid", shape=[], dtype=tf.float64),
    tf.TensorSpec(name="omegaRatioAsk", shape=[], dtype=tf.float64),
    tf.TensorSpec(name="side", shape=[], dtype=tf.int32),
    tf.TensorSpec(name="bidSize", shape=[], dtype=tf.int32),
    tf.TensorSpec(name="askSize", shape=[], dtype=tf.int32),
    tf.TensorSpec(name="spread", shape=[], dtype=tf.int32),
    tf.TensorSpec(name="marketHoldings", shape=[], dtype=tf.int32),
    tf.TensorSpec(name="contractHoldings", shape=[], dtype=tf.float64),
    tf.TensorSpec(name="numTransactions", shape=[], dtype=tf.int32),
    tf.TensorSpec(name="timeTilEnd", shape=[], dtype=tf.int64),
    tf.TensorSpec(name="latency", shape=[], dtype=tf.int64),
    tf.TensorSpec(name="timeSinceLastTrade", shape=[], dtype=tf.int64),
    tf.TensorSpec(name="bidVector", shape=[None], dtype=tf.float64),
    tf.TensorSpec(name="askVector", shape=[None], dtype=tf.float64),
    tf.TensorSpec(name="transactionHistory", shape=[None], dtype=tf.float64),
]


class TensorFlowAgent(BaseAgent, tf.Module):
    """Interface for a TensorFlow agent in Market-Sim."""

    def policy(
        self,
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
        """Pythonic interface to the underlying policy (implemented in TensorFlow).

        TODO: Describe policy inputs.

        Args:
            is_training:
            final_fundamental_estimate:
            private_bid:
            private_ask:
            omega_ratio_bid:
            omega_ratio_ask:
            side:
            bid_size:
            ask_size:
            spread:
            market_holdings:
            contract_holdings:
            num_transactions:
            time_til_end:
            latency:
            time_since_last_trade:
            bid_vector:
            ask_vector:
            transaction_history:
        Returns:
            (price, side, size)
        """
        # TODO: This is where any pre-processing and/or input checking would occur
        # however, we must not rely on operations done here. We will likely never
        # directly call this function in Python.
        return self._policy(
            is_training=is_training,
            final_fundamental_estimate=final_fundamental_estimate,
            private_bid=private_bid,
            private_ask=private_ask,
            omega_ratio_bid=omega_ratio_bid,
            omega_ratio_ask=omega_ratio_ask,
            side=side,
            bid_size=bid_size,
            ask_size=ask_size,
            spread=spread,
            market_holdings=market_holdings,
            contract_holdings=contract_holdings,
            num_transactions=num_transactions,
            time_til_end=time_til_end,
            latency=latency,
            time_since_last_trade=time_since_last_trade,
            bid_vector=bid_vector,
            ask_vector=ask_vector,
            transaction_history=transaction_history,
        )

    @tf.function(input_signature=BASE_POLICY_INPUT_TENSOR_SPEC)
    def _policy(
        self,
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
        bid_vector: typing.List[float],
        ask_vector: typing.List[float],
        transaction_history: typing.List[float],
    ) -> (float, int, int):
        """TensorFlow graph representing the policy of the agent.

        Please see `TensorFlowAgent.policy` for parameter descriptions, and
        `BASE_POLICY_INPUT_TENSOR_SPEC` for their types.

        When overriding this method it's important to assign the final three
        output's their respective names: `side`, `price`, `size`. One way to assign
        a name to an existing tensor is as follows:
        ```python
        price = ...
        ...
        price = tf.identity(price, name="price")
        ```
        """
        raise NotImplementedError()

    def save(self, path) -> None:
        """Save the agent's TF graphs to disk.

        Args:
            path: Path to new directory to save agent to.
        """
        tf.saved_model.save(self, path)
