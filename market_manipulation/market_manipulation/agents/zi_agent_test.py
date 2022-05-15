from zi_agent import ZIAgent
# import tensorflow as tf

agent = ZIAgent(
    Rmin=0,
    Rmax=1000,
    threshold=0.8,
    ordersPerSide=1,
    maxPosition=10,
)

file_path = r"C:\Users\Esoba\PycharmProjects\SRG_Summer_Project\market-sim\market-sim\zi_test"
agent.save(file_path)
