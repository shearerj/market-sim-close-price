""" Tensorboard events utility functions. """
import glob
import os.path as osp
from typing import Sequence, Tuple, Union

from tensorflow.python.summary.summary_iterator import summary_iterator


def value_iterator(tag, path, include_step: bool = False):

    for event in summary_iterator(path):
        for value in event.summary.value:
            if value.tag == tag:
                if include_step:
                    yield event.step, value.simple_value
                else:
                    yield value.simple_value


def get_all_summaries(path, include_step: bool = False):
    metrics = {}

    for event in summary_iterator(path):
        for value in event.summary.value:
            if value.tag not in metrics:
                metrics[value.tag] = []

            if include_step:
                metrics[value.tag] += [(event.step, value.simple_value)]
            else:
                metrics[value.tag] += [value.simple_value]

    # Format dictionary as: Metric --> X, Y.
    if include_step:
        for metric, data in metrics.items():
            xs = [d[0] for d in data]
            ys = [d[1] for d in data]
            metrics[metric] = (xs, ys)

    return metrics


def get_value_list(
    event_filepath: str, value_name: str, include_step: bool = False
) -> Union[Sequence[float], Tuple[Sequence[float], Sequence[int]]]:
    """Get a list of all values of a tag from an event file.

    Args:
        event_filepath: Path to event file.
        value_name: Tag name.

    Returns:
        If `include_step` is false returns a list of all the values from a file in
        the order that they appear. If it is true, then it also returns another list
        which contains the corresponding steps when each value was logged.
    """
    values = []
    steps = []

    for step, value in value_iterator(value_name, event_filepath, include_step=True):
        values += [value]
        steps += [step]

    if include_step:
        return values, steps
    else:
        return values


def get_max_and_final_value(event_filepath, value_name):
    """Get the maximum and final value of a tag from an event file.

    Args:
        event_filepath: Path of event file.
        value_name: Tag name.

    Returns:
        Tuple of max and final value.
    """
    best = float("-inf")
    final_step = 0
    final_value = 0.0

    for step, value in value_iterator(value_name, event_filepath, include_step=True):
        best = max(best, value)
        if step > final_step:
            final_step = step
            final_value = value

    return best, final_value


def get_event_filepath(run_dir: str) -> str:
    """Get the filepath of an event file within a directory.

    Args:
        run_dir: Directory that contains exactly one event file.

    Returns:
        Event filepath within `run_dir`.
    """
    event_filepath = osp.join(run_dir, "events.*")
    event_filepath = glob.glob(event_filepath)
    assert len(event_filepath) > 0, f"Did not find any event files at: {run_dir}"
    assert len(event_filepath) < 2, f"Found {len(event_filepath)} event files, expected 1."
    return event_filepath[0]
