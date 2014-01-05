package entity.infoproc;

import entity.market.Market;
import event.TimeStamp;

/**
 * Class that updates pertinent information for the system. Generally used for 
 * creating NBBO update events. Serves the purpose of the Information Processor 
 * for a single market.
 * 
 * @author cnris
 */
public class MarketQuoteProcessor extends AbstractQuoteProcessor {

	private static final long serialVersionUID = 827960237754648780L;
	
	public MarketQuoteProcessor(TimeStamp latency, Market associatedMarket) {
		super(latency, associatedMarket);
	}

	public String toString() {
		return "(QuoteProcessor " + id + " in " + associatedMarket + ")";
	}

}