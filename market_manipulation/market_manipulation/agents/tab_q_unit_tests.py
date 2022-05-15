import unittest
from market_manipulation.agents.tab_q_tfa import TabularQ
from market_manipulation.experience_parser import flatten_state_dict
import tensorflow as tf
from market_manipulation.utils.schedules import ConstantSchedule

# Create a sample state dictionaries from run of market-sim
state0_dict = {"finalFundamentalEstimate": 999999.9999999678,
               "privateBid": -1596.2878289561004,
               "privateAsk": 1663.8007408073713,
               "omegaRatioBid": 5.372140707583139,
               "omegaRatioAsk": 0.03081366753225501,
               "side": -1,
               "bidSize": 5,
               "askSize": 0,
               "spread": 7907,
               "marketHoldings": 2,
               "contractHoldings": 0,
               "numTransactions": 5,
               "timeTilEnd": 494,
               "latency": 0,
               "timeSinceLastTrade": 63,
               "bidVector": [
                                -9486.999999967753,
                                -9486.999999967753,
                                -9486.999999967753,
                                -9486.999999967753,
                                -9486.999999967753,
                                -9486.999999967753,
                                -9486.999999967753,
                                -9486.999999967753,
                                -9486.999999967753,
                                -9486.999999967753,
                                -9486.999999967753,
                                -9486.999999967753,
                                -9486.999999967753,
                                -9486.999999967753,
                                -9486.999999967753,
                                -4912.999999967753,
                                -2368.999999967753,
                                -1138.999999967753,
                                -1076.999999967753,
                                1580.000000032247
                            ],
               "askVector": [
                                9487.000000032247,
                                9487.000000032247,
                                9487.000000032247,
                                9487.000000032247,
                                9487.000000032247,
                                9487.000000032247,
                                9487.000000032247,
                                9487.000000032247,
                                9487.000000032247,
                                9487.000000032247,
                                9487.000000032247,
                                9487.000000032247,
                                9487.000000032247,
                                9487.000000032247,
                                9487.000000032247,
                                9487.000000032247,
                                9487.000000032247,
                                9487.000000032247,
                                9487.000000032247,
                                9487.000000032247
                            ],
               "transactionHistory": [
                                -418.99999996775296,
                                1179.000000032247,
                                1979.000000032247,
                                -1276.999999967753,
                                -3056.999999967753,
                                3.2247044146060944e-08,
                                3.2247044146060944e-08,
                                3.2247044146060944e-08,
                                3.2247044146060944e-08,
                                3.2247044146060944e-08,
                                3.2247044146060944e-08,
                                3.2247044146060944e-08,
                                3.2247044146060944e-08,
                                3.2247044146060944e-08,
                                3.2247044146060944e-08,
                                3.2247044146060944e-08,
                                3.2247044146060944e-08,
                                3.2247044146060944e-08,
                                3.2247044146060944e-08,
                                3.2247044146060944e-08
                            ]
               }

action = 1001920  # The price associated with this sample
zi_id = 0

state1_dict = {"finalFundamentalEstimate": 999999.261647408,
               "privateBid": -1596.2878289561004,
               "privateAsk": 1663.8007408073713,
               "omegaRatioBid": 8.79959839585337,
               "omegaRatioAsk": 0.02104926505444095,
               "side": 1,
               "bidSize": 2,
               "askSize": 4,
               "spread": 1430,
               "marketHoldings": 2,
               "contractHoldings": 0,
               "numTransactions": 8,
               "timeTilEnd": 144,
               "latency": 0,
               "timeSinceLastTrade": 122,
               "bidVector": [
                                -9487.261647408013,
                                -9487.261647408013,
                                -9487.261647408013,
                                -9487.261647408013,
                                -9487.261647408013,
                                -9487.261647408013,
                                -9487.261647408013,
                                -9487.261647408013,
                                -9487.261647408013,
                                -9487.261647408013,
                                -9487.261647408013,
                                -9487.261647408013,
                                -9487.261647408013,
                                -9487.261647408013,
                                -9487.261647408013,
                                -9487.261647408013,
                                -9487.261647408013,
                                -9487.261647408013,
                                -1272.2616474080132,
                                -1257.2616474080132
                            ],
               "askVector": [
                                172.73835259198677,
                                1616.7383525919868,
                                2366.7383525919868,
                                3451.7383525919868,
                                9486.738352591987,
                                9486.738352591987,
                                9486.738352591987,
                                9486.738352591987,
                                9486.738352591987,
                                9486.738352591987,
                                9486.738352591987,
                                9486.738352591987,
                                9486.738352591987,
                                9486.738352591987,
                                9486.738352591987,
                                9486.738352591987,
                                9486.738352591987,
                                9486.738352591987,
                                9486.738352591987,
                                9486.738352591987
                            ],
               "transactionHistory": [
                                -1076.2616474080132,
                                -296.26164740801323,
                                1580.7383525919868,
                                -418.26164740801323,
                                1179.7383525919868,
                                1979.7383525919868,
                                -1276.2616474080132,
                                -3056.2616474080132,
                                -0.26164740801323205,
                                -0.26164740801323205,
                                -0.26164740801323205,
                                -0.26164740801323205,
                                -0.26164740801323205,
                                -0.26164740801323205,
                                -0.26164740801323205,
                                -0.26164740801323205,
                                -0.26164740801323205,
                                -0.26164740801323205,
                                -0.26164740801323205,
                                -0.26164740801323205
                            ]
               }

reward = -1.4767051194794476  # Reward from the transaction in sample market-sim
done = 0


class TestTrainingMethods(unittest.TestCase):

    def test_marketsim_sample(self):
        # Simulate experience parser for one experience
        s0 = flatten_state_dict(state0_dict)
        a = action
        zid = zi_id
        s1 = flatten_state_dict(state1_dict)
        r = reward
        d = done
        parsed_experience = [(s0, a, zid, s1, r, d)]

        # Define the TabularQ agent
        agent = TabularQ(batch_size=1,
                         discount=.99,
                         replay_capacity=10,
                         min_replay_size=0,
                         learning_rate=.01,
                         exploration_schedule=ConstantSchedule(x=.03)
                         )
        # Test the policy of Tabular Q Agent
        price, side, size, rmin, rmax, thresh = agent.policy(s0[0], s0[1], s0[2], s0[3], s0[4],
                                                             tf.cast(s0[5], dtype=tf.int32), tf.cast(s0[6], dtype=tf.int32),
                                                             tf.cast(s0[7], dtype=tf.int32), tf.cast(s0[8], dtype=tf.int32),
                                                             tf.cast(s0[9], dtype=tf.int32), s0[10],
                                                             tf.cast(s0[11], dtype=tf.int32), tf.cast(s0[12], dtype=tf.int64),
                                                             tf.cast(s0[13], dtype=tf.int64), tf.cast(s0[14], dtype=tf.int64),
                                                             s0[15:15 + 20],
                                                             s0[15 + 20:15 + 2 * 20],
                                                             s0[15 + 2 * 20:15 + 3 * 20]
                                                             )
        assert rmin == 0
        assert rmax == 450
        assert thresh == .5
        # Test recording the experience into the replay buffer
        agent.record_experiences(parsed_experience)
        assert agent.replay.size == 1

        # Test update and _train step
        update_dict = agent.update()
        print(update_dict)
        print(agent.q_table)


if __name__ == '__main__':
    unittest.main()