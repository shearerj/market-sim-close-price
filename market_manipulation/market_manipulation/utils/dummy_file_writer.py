class DummyFileWriter(object):
    """A fake file writer that writes nothing to the disk."""

    def __init__(self, logdir):
        self._logdir = logdir

    def get_logdir(self):
        """Returns the directory where event file will be written."""
        return self._logdir

    def add_event(self, event, step=None, walltime=None):
        return

    def add_summary(self, summary, global_step=None, walltime=None):
        return

    def add_graph(self, graph_profile, walltime=None):
        return

    def add_onnx_graph(self, graph, walltime=None):
        return

    def flush(self):
        return

    def close(self):
        return

    def reopen(self):
        return

    def add_scalar(self, *args, **kwargs):
        return
