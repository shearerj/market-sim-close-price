package edu.umich.srg.marketsim;

public interface Sim {

  void scheduleIn(TimeStamp delay, Runnable activity);

  TimeStamp getCurrentTime();

}
