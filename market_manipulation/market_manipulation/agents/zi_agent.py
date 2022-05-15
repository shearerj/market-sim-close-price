import typing

import gin
import tensorflow as tf

import market_manipulation.agents.tensorflow_agent as tfa


@gin.configurable
class ZIAgent(tfa.TensorFlowAgent):
    """Market-Sim agent interface."""

    def __init__(
        self,
        Rmin: int,
        Rmax: int,
        threshold: float,
        ordersPerSide: int,
        maxPosition: int,
    ):
        """Constructor.

        Initialize all algorithm specific parameters and modules.

        Args:
            Rmin: Lower limit on uniform distribution for surplus
            Rmax: Upper limit on uniform distribution for surplus
            threshold: Fraction of surplus at which agent will decide to immediately trade
            ordersPerSide: How many orders an agent can place at trading time
            maxPosition: How many assets an agent can buy and sell at a time
        """
        super().__init__()
        self.Rmin = Rmin
        self.Rmax = Rmax
        self.threshold = threshold
        self.ordersPerSide = ordersPerSide
        self.maxPosition = maxPosition

    @tf.function(input_signature=tfa.BASE_POLICY_INPUT_TENSOR_SPEC)
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
        time_since_last_trade: int,
        bid_vector: typing.List[float],
        ask_vector: typing.List[float],
        transaction_history: typing.List[float],
    ):
        """Run inference the agent's policy.
        Returns:
            Action selected.
        """
        demandedSurplus = tf.random.uniform(shape=[], minval=self.Rmin, maxval=self.Rmax, dtype=tf.float64)
        holding_cond = abs(market_holdings + side) <= self.maxPosition  # Condition for holding restrictions
        privateBenefit = tf.where(side == 1, x=private_bid, y=private_ask)
        estimatedValue = tf.add(final_fundamental_estimate, privateBenefit)
        pre_bid_price = bid_vector[-1]
        pre_ask_price = ask_vector[0]
        price_no_op = tf.constant(-100, dtype=tf.float64)  # No-op
        bid_price = tf.where(bid_size == 0, x=price_no_op, y=pre_bid_price)
        ask_price = tf.where(ask_size == 0, x=price_no_op, y=pre_ask_price)
        toSubmit = self.shadePrice(self.threshold, side, bid_price, ask_price, estimatedValue, demandedSurplus)
        pre_rounded = tf.where(side == 1, x=tf.math.floor(toSubmit), y=tf.math.ceil(toSubmit))
        rounded_no_op = tf.constant(-1, dtype=tf.float64)
        rounded = tf.where(pre_rounded > 0, x=pre_rounded, y=rounded_no_op)
        rounded_final = tf.where(holding_cond, x=rounded, y=rounded_no_op)
        return rounded_final, side, tf.constant(1, dtype=tf.int32), self.Rmin, self.Rmax, self.threshold

    def shadePrice(self, threshold, shade_side, bidPrice, askPrice, estimatedValue, demandedSurplus):
        market_diff = tf.where(shade_side == 1, x=askPrice, y=bidPrice)
        ds = tf.maximum(demandedSurplus, 0)
        dst = tf.minimum(demandedSurplus, tf.multiply(threshold, demandedSurplus))
        market_cond1 = tf.less(market_diff, ds)
        market_cond2 = tf.greater(market_diff, dst)
        market_cond3 = tf.not_equal(market_diff, -100)
        market_cond = (market_cond1 and market_cond2) and market_cond3
        diff_multi = tf.multiply(tf.cast(shade_side, dtype=tf.float64), market_diff)
        market_price = tf.subtract(estimatedValue, diff_multi)
        ds_multi = tf.multiply(tf.cast(shade_side, dtype=tf.float64), demandedSurplus)
        mp_else = tf.subtract(estimatedValue, ds_multi)
        return tf.where(market_cond, x=market_price, y=mp_else)

    def save_tf_graph(self, path) -> None:
        """Save the agent's TF graphs to disk.

        Args:
            path:
        """
        pass

    def record_experiences(self, experiences) -> None:
        """Record a sequence of experiences into the agent's replay buffer.

        Args:
            experiences: List of experience tuples.
        """
        pass

    def update(self) -> dict:
        """Perform a training update on the agent."""
        pass
