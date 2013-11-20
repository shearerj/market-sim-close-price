package entity.agent;

import java.util.Collection;
import java.util.Map;
import java.util.Random;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.Maps;

import data.FundamentalValue;
import entity.infoproc.HFTIP;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Quote;
import event.TimeStamp;

/**
 * Abstract class for high-frequency traders. Creates the necessary information
 * processors and links them to the appropriate markets.
 * 
 * @author ewah
 */
public abstract class HFTAgent extends MMAgent {

	private static final long serialVersionUID = -1483633963238206201L;
	
	protected final Map<Market, HFTIP> ips;

	public HFTAgent(TimeStamp latency, TimeStamp arrivalTime,
			FundamentalValue fundamental, SIP sip, Collection<Market> markets, 
			Random rand, int tickSize) {
		super(arrivalTime, fundamental, sip, markets, rand, tickSize);
		this.ips = Maps.newHashMap();
		
		for (Market market : markets) {
			HFTIP laip = new HFTIP(latency, market, this);
			ips.put(market, laip);
			market.addIP(laip);
		}
	}
	
	/**
	 * Get quote in the specified market.
	 * @param market
	 * @return
	 */
	public Quote getQuote(Market market) {
		checkNotNull(market);
		if (ips.containsKey(market)) return ips.get(market).getQuote();
		return new Quote(null, null, 0, null, 0, TimeStamp.ZERO);
	}
}
