package edu.umich.srg.marketsim.fundamental;

import java.io.Serializable;

import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.TimeStamp;

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

}
