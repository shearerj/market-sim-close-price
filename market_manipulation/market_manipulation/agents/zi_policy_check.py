from zi_agent import ZIAgent
import tensorflow as tf
agent = ZIAgent(
    Rmin=0,
    Rmax=1000,
    threshold=0.8,
    ordersPerSide=1,
    maxPosition=10,
    privateValueVar=50000000,
    arrivalRate=0.005
)
print(agent._policy(tf.constant(1000000.0, dtype=tf.float64), tf.constant(1322.6440133720457, dtype=tf.float64),
              tf.constant(2860.2701493826676, dtype=tf.float64), tf.constant(1.0, dtype=tf.float64),
              tf.constant(1.0, dtype=tf.float64), tf.constant(-1), tf.constant(0), tf.constant(1),
              tf.constant(12805), tf.constant(0), tf.constant(0.0, dtype=tf.float64), tf.constant(1),
              tf.constant(886, dtype=tf.int64), tf.constant(0, dtype=tf.int64), tf.constant(30, dtype=tf.int64),
              tf.constant([
                  -9487.0,
                  -9487.0,
                  -9487.0,
                  -9487.0,
                  -9487.0,
                  -9487.0,
                  -9487.0,
                  -9487.0,
                  -9487.0,
                  -9487.0,
                  -9487.0,
                  -9487.0,
                  -9487.0,
                  -9487.0,
                  -9487.0,
                  -9487.0,
                  -9487.0,
                  -9487.0,
                  -9487.0,
                  -9487.0
              ], dtype=tf.float64),
              tf.constant([
                  3318.0,
                  9487.0,
                  9487.0,
                  9487.0,
                  9487.0,
                  9487.0,
                  9487.0,
                  9487.0,
                  9487.0,
                  9487.0,
                  9487.0,
                  9487.0,
                  9487.0,
                  9487.0,
                  9487.0,
                  9487.0,
                  9487.0,
                  9487.0,
                  9487.0,
                  9487.0
              ], dtype=tf.float64),
              tf.constant([-3057.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
               0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0], dtype=tf.float64)
              )  # Should return 9511828 (or ballpark due to the random nature of the private values)
      )