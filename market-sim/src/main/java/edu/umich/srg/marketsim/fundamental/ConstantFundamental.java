package edu.umich.srg.marketsim.fundamental;

import com.google.gson.JsonObject;

import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.TimeStamp;

import java.io.Serializable;

public class ConstantFundamental implements Fundamental, Serializable {

  private final Price constant;

  private ConstantFundamental(Price constant) {
    this.constant = constant;
  }

  public static ConstantFundamental create(Price constant) {
    return new ConstantFundamental(constant);
  }

  public static ConstantFundamental create(Number constant) {
    return new ConstantFundamental(Price.of(constant.doubleValue()));
  }

  @Override
  public Price getValueAt(TimeStamp time) {
    return constant;
  }

  private static final long serialVersionUID = 1;

  @Override
  public JsonObject getFeatures() {
    return new JsonObject();
  }

}
