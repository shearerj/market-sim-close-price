package edu.umich.srg.marketsim.testing;

import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.TimeStamp;

public class MockSim implements Sim {

  @Override
  public void scheduleIn(TimeStamp delay, Runnable activity) {

  }

  @Override
  public TimeStamp getCurrentTime() {
    return TimeStamp.ZERO;
  }

}
