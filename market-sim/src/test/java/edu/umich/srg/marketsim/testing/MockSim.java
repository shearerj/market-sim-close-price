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

  @Override
  public void error(String format, Object... parameters) {
    // TODO Auto-generated method stub

  }

  @Override
  public void info(String format, Object... parameters) {
    // TODO Auto-generated method stub

  }

  @Override
  public void debug(String format, Object... parameters) {
    // TODO Auto-generated method stub

  }

  @Override
  public void addFeature(String name, double value) {
    // TODO Auto-generated method stub

  }

}
