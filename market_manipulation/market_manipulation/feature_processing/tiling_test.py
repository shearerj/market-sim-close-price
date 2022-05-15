from tiling import create_features, encode
import tensorflow as tf

"""
# Constant input below
side = tf.constant(1, dtype=tf.int32)
omega_ratio_bid = tf.constant(1, dtype=tf.float64)
omega_ratio_ask = tf.constant(1, dtype=tf.float64)
private_bid = tf.constant(100, dtype=tf.float64)
private_ask = tf.constant(100, dtype=tf.float64)
time_since_last_trade = tf.constant(10, dtype=tf.int32)
bid_vector = tf.constant(([1, -1, 1, -1, -1]), dtype=tf.float64)
ask_vector = tf.constant(([1, -1, 1, -1, -1]), dtype=tf.float64)
ask_size = tf.constant(1, dtype=tf.int32)
bid_size = tf.constant(1, dtype=tf.int32)
final_fundamental_estimate = tf.constant(1, dtype=tf.float64)
spread = tf.constant(1, dtype=tf.int32)
market_holdings = tf.constant(1, dtype=tf.int32)
contract_holdings = tf.constant(1, dtype=tf.float64)
num_transactions = tf.constant(1, dtype=tf.int32)
time_til_end = tf.constant(1, dtype=tf.int32)
latency = tf.constant(1, dtype=tf.int32)
transaction_history = tf.constant(([1, -1, 1, -1, -1]), dtype=tf.float64)"""


#Batch input below
side = tf.constant((1, -1, 1, -1, -1), dtype=tf.int32, shape=(5,))
omega_ratio_bid = tf.constant((1, 0, 2, 1, 0), dtype=tf.float64, shape=(5,))
omega_ratio_ask = tf.constant((1, 0, 2, 1, 0), dtype=tf.float64, shape=(5,))
private_bid = tf.constant((100, 0, 200, 100, 0), dtype=tf.float64, shape=(5,))
private_ask = tf.constant((100, 0, 200, 100, 0), dtype=tf.float64, shape=(5,))
time_since_last_trade = tf.constant((100, 10, 10, 10, 10), dtype=tf.int32, shape=(5,))
bid_vector = tf.constant(([1, -1, 1, -1, -1], [1, -10, 1, -1, -1], [1, -1, 1, -1, -1],
                          [1, -1, 1, -1, -1], [1, -1, 1, -1, -1]), dtype=tf.float64, shape=(5, 5))
ask_vector = tf.constant(([1, -1, 1, -1, -1], [1, -1, 1, -1, -1], [1, -1, 1, -1, -1],
                          [1, -1, 1, -1, -1], [1, -1, 1, -1, -1]), dtype=tf.float64, shape=(5, 5))
ask_size = tf.constant((1, 1, 1, 1, 1), dtype =tf.int32, shape=(5,))
bid_size = tf.constant((0, 0, 0, 0, 0), dtype=tf.int32, shape=(5,))
final_fundamental_estimate = tf.constant((1, 0, 2, 1, 0), dtype=tf.float64, shape=(5,))
spread = tf.constant((1, 1, 1, 1, 1), dtype=tf.int32, shape=(5,))
market_holdings = tf.constant((1, 1, 1, 1, 1), dtype=tf.int32, shape=(5,))
contract_holdings = tf.constant((1, 1, 1, 1, 1), dtype=tf.float64, shape=(5,))
num_transactions = tf.constant((1, 1, 1, 1, 1), dtype=tf.int32, shape=(5,))
time_til_end = tf.constant((1, 1, 1, 1, 1), dtype=tf.int32, shape=(5,))
latency = tf.constant((1, 1, 1, 1, 1), dtype=tf.int32, shape=(5,))
transaction_history = tf.constant(([1, -1, 1, -1, -1], [1, -10, 1, -1, -1], [1, -1, 1, -1, -1],
                                   [1, -1, 1, -1, -1], [1, -1, 1, -1, -1]), dtype=tf.float64, shape=(5, 5))

test_parse = create_features(final_fundamental_estimate, private_bid, private_ask, omega_ratio_bid, omega_ratio_ask,
                             side, bid_size, ask_size, time_since_last_trade, bid_vector, ask_vector)
encoded = encode(test_parse)
print(encoded)
