import typing
import tensorflow as tf

# Functions designed to work inside tensorflow graph decorated with tf.function


def p_bin(profit):
    """Partition the profit into bins, accepts tensors of size = () or size = (batch_size, 1)
    :return
    scalar bin value or vector of bin values"""

    zero_cond = profit <= tf.constant(-250.0, dtype=tf.float64)  # Condition for bin 0
    one_cond = tf.math.logical_and(profit > tf.constant(-250.0, dtype=tf.float64),  # Condition for bin 1
                                   profit <= tf.constant(0.0, dtype=tf.float64))
    two_cond = profit > tf.constant(0, dtype=tf.float64)  # Condition for bin 2

    zeros_value = tf.where(zero_cond, x=1, y=0)  # The y=0 so that addition of vectors produces mutually exclusive bins
    ones_value = tf.where(one_cond, x=2, y=0)
    twos_value = tf.where(two_cond, x=3, y=0)
    return zeros_value+ones_value+twos_value-tf.constant(1, dtype=tf.int32)  # Subtract 1 for proper radix conversion


def o_bin(omega_ratio):
    """Partition the omega ratio into bins, accepts tensors of size = () or size = (batch_size, 1)
    :return
    scalar bin value or vector of bin values"""
    zero_cond = omega_ratio <= tf.constant(1, dtype=tf.float64)  # Condition for bin 0
    one_cond = tf.math.logical_and(omega_ratio > tf.constant(1, dtype=tf.float64),  # Condition for bin 1
                                   omega_ratio <= tf.constant(6, dtype=tf.float64))
    two_cond = omega_ratio > tf.constant(6, dtype=tf.float64)  # Condition for bin 2

    zeros_value = tf.where(zero_cond, x=1, y=0)  # The y=0 so that addition of vectors produces mutually exclusive bins
    ones_value = tf.where(one_cond, x=2, y=0)
    twos_value = tf.where(two_cond, x=3, y=0)
    return zeros_value+ones_value+twos_value-tf.constant(1, dtype=tf.int32)  # Subtract 1 for proper radix conversion


def v_bin(private_value):
    """Partition the private value into bins, accepts tensors of size = () or size = (batch_size, 1)
    :return
    scalar bin value or vector of bin values"""
    zero_cond = private_value <= tf.constant(-900, dtype=tf.float64)  # Condition for bin 0
    one_cond = tf.math.logical_and(private_value > tf.constant(-900, dtype=tf.float64),  # Condition for bin 1
                                   private_value <= tf.constant(500, dtype=tf.float64))
    two_cond = private_value > tf.constant(500, dtype=tf.float64)  # Condition for bin 2

    zeros_value = tf.where(zero_cond, x=1, y=0)  # The y=0 so that addition of vectors produces mutually exclusive bins
    ones_value = tf.where(one_cond, x=2, y=0)
    twos_value = tf.where(two_cond, x=3, y=0)
    return zeros_value+ones_value+twos_value-tf.constant(1, dtype=tf.int32)  # Subtract 1 for proper radix conversion


def d_bin(dur):
    """Partition the duration into bins, accepts tensors of size = () or size = (batch_size, 1)
    :return
    scalar bin value or vector of bin values"""
    zero_cond = dur <= tf.constant(50, dtype=tf.float64)
    one_cond = tf.math.logical_and(dur > tf.constant(50, dtype=tf.float64), dur <= tf.constant(200, dtype=tf.float64))
    two_cond = dur > tf.constant(200, dtype=tf.float64)

    zeros_value = tf.where(zero_cond, x=1, y=0)  # The y=0 so that addition of vectors produces mutually exclusive bins
    ones_value = tf.where(one_cond, x=2, y=0)
    twos_value = tf.where(two_cond, x=3, y=0)
    return zeros_value+ones_value+twos_value-tf.constant(1, dtype=tf.int32)  # Subtract 1 for proper radix conversion


def tile_features(feature_tensor):
    """Places POVD features in their proper bins for encoding, accepts tensors of size = (4,) or size = (batch_size,4)
    :return
    vector (size = (4,)) of binned values or matrix (size = (batch_size,4)) of binned values"""
    copy_tsp = tf.transpose(feature_tensor)  # Transpose the matrix so elements work with vector or matrix input
    p = p_bin(copy_tsp[0])  # Put the profit values in their proper bins
    o = o_bin(copy_tsp[1])  # Put the omega ratio in their proper bins
    v = v_bin(copy_tsp[2])  # Put the private value in their proper bins
    d = d_bin(copy_tsp[3])  # Put the duration in their proper bins
    stacked = tf.transpose(tf.stack([p, o, v, d], axis=0))  # Stack features and transpose to return [p,o,v,d] on rows
    return stacked


def create_features(
        final_fundamental_estimate: float,
        private_bid: float,
        private_ask: float,
        omega_ratio_bid: float,
        omega_ratio_ask: float,
        side: int,
        bid_size: int,
        ask_size: int,
        time_since_last_trade: int,
        bid_vector: typing.List[float],
        ask_vector: typing.List[float],
        ):
    """Takes arguments from TensorSpec to create a POVD tensor, accepts tensors of size = () or size = (batch,1)
    :return
    vector (size = (4,)) or matrix (size = (batch_size,1,4), the 1 comes out in future tensor operations)"""
    isBuyer = side == 1  # Boolean scalar of vector determining whether the agent was buying or selling
    # tf.where --> Returns x when condition is true, y when false
    omega_ratio = tf.transpose(tf.where(isBuyer, x=omega_ratio_bid, y=omega_ratio_ask))
    private_value = tf.transpose(tf.where(isBuyer, x=private_bid, y=private_ask))
    duration = tf.transpose(tf.cast(time_since_last_trade, dtype=tf.float64))
    # Values are transposed so operations are able to work with both scalars and matrices with batch size dimension
    bid_reduce = tf.math.reduce_min(tf.transpose(bid_vector), axis=0)
    ask_reduce = tf.math.reduce_max(tf.transpose(ask_vector), axis=0)
    profit_bid = bid_reduce - tf.transpose(private_bid + final_fundamental_estimate)
    profit_ask = ask_reduce - tf.transpose(private_ask + final_fundamental_estimate)
    profit_bid = tf.transpose(profit_bid)
    profit_ask = tf.transpose(profit_ask)
    pre_profit = tf.where(isBuyer, x=profit_bid, y=profit_ask)
    zero_check1 = tf.math.logical_and(ask_size == 0, isBuyer)  # Bool to determine if profit should default to 0
    zero_check2 = tf.math.logical_and(bid_size == 0,
                                      tf.math.logical_not(isBuyer))  # Bool to determine if profit should default to 0
    zero_check = tf.math.logical_or(zero_check1, zero_check2)  # Either check satisfies profit = 0 condition
    profit = tf.transpose(tf.where(zero_check, x=tf.constant(0, dtype=tf.float64), y=pre_profit))  # Replace where true
    return tf.transpose(tf.stack([profit, omega_ratio, private_value, duration], axis=0))  # Features to be binned


def encode(batch):
    """Maps features to an integer using a radix conversion, accepts tensors of size = (4,) or size = (batch_size,4)
    :return
    scalar coded integer or vector of coded integers"""
    coded = tile_features(batch)  # Turn features into bins, see tile_features
    radix = tf.constant(([27, 9, 3, 1]), dtype=tf.int32, shape=(4,))  # Base 3 radix conversion
    return tf.reduce_sum(tf.transpose(radix*coded), axis=0)  # Radix conversion between 0-80
