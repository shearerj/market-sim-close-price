import json

import tensorflow as tf


class ZScoreNormalizationBench:
    def __init__(self, pop_stats_path):
        f = open(pop_stats_path)
        self.pop_stats = json.load(f)
        self.pop_mean = []
        self.pop_var = []

    def set_mean_var(self, bid_len, ask_len, trade_len, max_vector_len):
        pop_mean = []
        pop_var = []

        pop_mean.append(self.pop_stats["Final fundamental estimate"]["Mean"])
        pop_var.append(self.pop_stats["Final fundamental estimate"]["Variance"])
        pop_mean.append(self.pop_stats["Private Bid"]["Mean"])
        pop_var.append(self.pop_stats["Private Bid"]["Variance"])
        pop_mean.append(self.pop_stats["Private Ask"]["Mean"])
        pop_var.append(self.pop_stats["Private Ask"]["Variance"])
        pop_mean.append(self.pop_stats["Omega Bid Ratio"]["Mean"])
        pop_var.append(self.pop_stats["Omega Bid Ratio"]["Variance"])
        pop_mean.append(self.pop_stats["Omega Ask Ratio"]["Mean"])
        pop_var.append(self.pop_stats["Omega Ask Ratio"]["Variance"])
        pop_mean.append(self.pop_stats["Side"]["Mean"])
        pop_var.append(self.pop_stats["Side"]["Variance"])
        pop_mean.append(self.pop_stats["Bid Size"]["Mean"])
        pop_var.append(self.pop_stats["Bid Size"]["Variance"])
        pop_mean.append(self.pop_stats["Ask Size"]["Mean"])
        pop_var.append(self.pop_stats["Ask Size"]["Variance"])
        pop_mean.append(self.pop_stats["Spread"]["Mean"])
        pop_var.append(self.pop_stats["Spread"]["Variance"])
        pop_mean.append(self.pop_stats["Market Holdings"]["Mean"])
        pop_var.append(self.pop_stats["Market Holdings"]["Variance"])
        pop_mean.append(self.pop_stats["Contract Holdings"]["Mean"])
        pop_var.append(self.pop_stats["Contract Holdings"]["Variance"])
        pop_mean.append(self.pop_stats["Number of Transactions"]["Mean"])
        pop_var.append(self.pop_stats["Number of Transactions"]["Variance"])
        pop_mean.append(self.pop_stats["Time Until the End"]["Mean"])
        pop_var.append(self.pop_stats["Time Until the End"]["Variance"])
        # pop_mean.append(self.pop_stats["Latency"]["Mean"])
        # pop_var.append(self.pop_stats["Latency"]["Variance"])
        pop_mean.append(self.pop_stats["Time Since Last Trade"]["Mean"])
        pop_var.append(self.pop_stats["Time Since Last Trade"]["Variance"])

        for i in range(max_vector_len - bid_len, max_vector_len):
            pos = f"Bid Position {i}"
            pop_mean.append(self.pop_stats[pos]["Mean"])
            pop_var.append(self.pop_stats[pos]["Variance"])

        for i in range(ask_len):
            pos = f"Ask Position {i}"
            pop_mean.append(self.pop_stats[pos]["Mean"])
            pop_var.append(self.pop_stats[pos]["Variance"])

        for i in range(trade_len):
            pos = f"Transactions Position {i}"
            pop_mean.append(self.pop_stats[pos]["Mean"])
            pop_var.append(self.pop_stats[pos]["Variance"])

        self.pop_mean = tf.cast(tf.reshape(pop_mean, [1, -1]), tf.float32)
        self.pop_sd = tf.math.sqrt(tf.cast(tf.reshape(pop_var, [1, -1]), tf.float32))

    def __call__(self, x, bid_len, ask_len, trade_len, max_vector_len):
        self.set_mean_var(bid_len, ask_len, trade_len, max_vector_len)
        y = tf.math.subtract(x, self.pop_mean)
        z = tf.math.divide_no_nan(y, self.pop_sd)
        return z
