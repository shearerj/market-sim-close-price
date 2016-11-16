package edu.umich.srg.marketsim.testing;

import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.TimeStamp;

public class MockSim implements Sim {

  private TimeStamp currentTime;

  public MockSim() {
    currentTime = TimeStamp.ZERO;
  }

  @Override
  public void scheduleIn(TimeStamp delay, Runnable activity) {}

  @Override
  public TimeStamp getCurrentTime() {
    return currentTime;
  }

  public void setTime(long newTime) {
    currentTime = TimeStamp.of(newTime);
  }

}
