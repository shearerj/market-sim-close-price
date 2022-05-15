from dqn_agent import DQNAgent
import tensorflow as tf

pi = DQNAgent(
    network=None,
    optimizer=None,
    batch_size=1,
    replay_capacity=10,
    min_replay_size=1,
    target_update_period=1,
    exploration_schedule=None,
)

pi.epsilon = 0.03
pi.save("test")  # Saves folder with the path name that has saved model (.pb extension)
# tf.saved_model.load("C:/Users/Esoba/PycharmProjects/SRG_Summer_Project/market_manipulation/agents/steve")
# Manually copy folders over to the market-sim
