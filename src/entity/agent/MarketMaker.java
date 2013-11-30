package entity.agent;

import iterators.ExpInterarrivals;

import java.util.Iterator;
import java.util.Random;

import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import event.TimeStamp;

/**
 * Abstract class for MarketMakers.
 * 
 * @author ewah
 */
public abstract class MarketMaker extends ReentryAgent {

	private static final long serialVersionUID = -782740037969385370L;
	
	public MarketMaker(FundamentalValue fundamental, SIP sip, Market market,
			Random rand, Iterator<TimeStamp> reentry, int tickSize) {
		super(TimeStamp.ZERO, fundamental, sip, market, rand, reentry, tickSize);
	}
	
	/**
	 * Shortcut constructor for exponential interarrivals (e.g. Poisson reentries)
	 */
	public MarketMaker(FundamentalValue fundamental, SIP sip,
			Market market, Random rand, double reentryRate, int tickSize) {
		this(fundamental, sip, market, rand, new ExpInterarrivals(reentryRate, rand),
				tickSize);
	}
}
