""" Exploration schedules. """
from dataclasses import dataclass

import gin


class Schedule(object):
    def value(self, t: int) -> float:
        """Value of the schedule at time t."""
        raise NotImplementedError()


@gin.configurable
@dataclass
class ConstantSchedule(Schedule):

    x: float

    def value(self, t: int) -> float:
        return self.x


@gin.configurable
@dataclass
class LinearSchedule(Schedule):

    initial_x: float
    final_x: float
    num_timesteps: int

    def value(self, t: int) -> float:
        fraction = min(float(t) / self.num_timesteps, 1.0)
        return self.initial_x + fraction * (self.final_x - self.initial_x)
