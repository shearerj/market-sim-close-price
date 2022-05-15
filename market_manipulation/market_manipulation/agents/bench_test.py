from bench import BENCH

pi = BENCH(
    critic_optimizer=None,
    actor_optimizer=None,
    batch_size=1,
    replay_capacity=10,
    min_replay_size=1,
    target_update_period=1,
    gamma=0.99,
    tau=0.005,
    exploration_schedule=None,
)

pi.epsilon = 0.03
pi.save("test")
