""" Methods for stopping training early.

We assume that all early-stopping methods are callable, and they are given as input
the metrics that they should use to determine if training should end.
"""
import logging
from dataclasses import dataclass

import gin
import numpy as np

logger = logging.getLogger(__name__)


@gin.configurable
def never(*args, **kwargs) -> bool:
    """Never stop training early.

    Returns:
        False.
    """
    return False


@gin.configurable
@dataclass
class NoMeanImprovement:
    """Stops if the mean performance stabilizes."""

    short_horizon: int = 10
    medium_horizon: int = 20
    long_horizon: int = 50

    delta: float = 0.1

    def __post_init__(self):
        self.episodic_performances = []
        self.ended = False

    def __call__(self, performance, *args, **kwargs):
        self.episodic_performances += [performance]
        assert not self.ended

        if len(self.episodic_performances) < self.long_horizon:
            return False

        # Limit data usage.
        if len(self.episodic_performances) > self.long_horizon:
            self.episodic_performances = self.episodic_performances[-self.long_horizon :]

        # Ensure that the performance has remained steady over time.
        short_perf = np.mean(self.episodic_performances[-self.short_horizon :])
        med_perf = np.mean(self.episodic_performances[-self.medium_horizon :])
        long_perf = np.mean(self.episodic_performances)

        if np.absolute(short_perf - med_perf) > self.delta:
            return False
        if np.absolute(med_perf - long_perf) > self.delta:
            return False

        logger.info("Training ended early.")
        logger.info(f"No mean improvement: {short_perf}, {med_perf}, {long_perf}")
        self.ended = True
        return True


@gin.configurable
@dataclass
class NoStdImprovement:
    """Stops if the standard deviation of performance stabilizes."""

    short_horizon: int = 10
    medium_horizon: int = 50
    long_horizon: int = 100

    delta: float = 0.01

    def __post_init__(self):
        self.episodic_performances = []
        self.ended = False

    def __call__(self, performance, *args, **kwargs):
        self.episodic_performances += [performance]
        assert not self.ended

        if len(self.episodic_performances) < self.long_horizon:
            return False

        # Limit data usage.
        if len(self.episodic_performances) > self.long_horizon:
            self.episodic_performances = self.episodic_performances[-self.long_horizon :]

        # Ensure that the performance has remained steady over time.
        short_perf = np.std(self.episodic_performances[-self.short_horizon :])
        med_perf = np.std(self.episodic_performances[-self.medium_horizon :])
        long_perf = np.std(self.episodic_performances)

        if np.absolute(short_perf - med_perf) > self.delta:
            return False
        if np.absolute(med_perf - long_perf) > self.delta:
            return False

        self.ended = True
        return True
