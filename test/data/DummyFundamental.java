package data;

import market.Price;
import event.TimeStamp;
import utils.RandPlus;

/**
 * Dummy class to make unit testing of agents easier.
 * Will simply return meanVal regardless of time.
 * 
 * @author drhurd
 *
 */
public class DummyFundamental extends FundamentalValue{

	public DummyFundamental(double kap, int meanVal, double var, RandPlus rand) {
		super(kap, meanVal, var, rand);
		// TODO Auto-generated constructor stub
	}
	
	public Price getValueAt(TimeStamp t) {
		return new Price(meanValue);
	}

}
