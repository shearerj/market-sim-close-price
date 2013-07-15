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
public class DummyFundamental extends FundamentalValue {
	
	public DummyFundamental(int mean) {
		super(0, mean, 0, new RandPlus());
	}
	
	@Override
	public Price getValueAt(TimeStamp t) {
		return new Price(meanValue);
	}

}
