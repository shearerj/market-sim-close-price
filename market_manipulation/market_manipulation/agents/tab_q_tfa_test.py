from market_manipulation.agents.tab_q_tfa import TabularQ
import tensorflow as tf
from market_manipulation.utils.schedules import ConstantSchedule

agent = TabularQ(
            batch_size=10,
            discount=0.99,
            replay_capacity=10000,
            min_replay_size=100,
            learning_rate=0.003,
            exploration_schedule=ConstantSchedule(.3),  # Schedule not in use currently
        )

file_path = r"C:\Users\Esoba\PycharmProjects\SRG_Summer_Project\market-sim\market-sim\TabQ_test"
agent.save(file_path)
file_path2 = r"C:\Users\Esoba\PycharmProjects\SRG_Summer_Project\market_manipulation\TabQ_test"
agent.save(file_path2)