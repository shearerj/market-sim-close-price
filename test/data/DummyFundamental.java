package data;

import entity.market.Price;
import event.TimeStamp;
import utils.Rands;

/**
 * Dummy class to make unit testing of agents easier.
 * Will simply return meanVal regardless of time.
 * 
 * @author drhurd
 *
 */
public class DummyFundamental extends FundamentalValue {
	
	private static final long serialVersionUID = 1L;

	public DummyFundamental(int mean) {
		super(0, mean, 0, new Rands());
	}
	
	@Override
	public Price getValueAt(TimeStamp t) {
		return new Price(meanValue);
	}

}
