import sonnet as snt
from gin import config

config.external_configurable(snt.optimizers.SGD, module="snt.optimizers")
config.external_configurable(snt.optimizers.Adam, module="snt.optimizers")
