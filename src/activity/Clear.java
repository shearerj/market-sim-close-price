package activity;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;

import entity.market.Market;
import event.TimeStamp;

/**
 * Class for Activity of clearing the orderbook.
 * 
 * @author ewah
 */
public class Clear extends Activity {

	protected final Market market;

	public Clear(Market market, TimeStamp scheduledTime) {
		super(scheduledTime);
		this.market = checkNotNull(market, "Market");
	}

	@Override
	public Collection<? extends Activity> execute(TimeStamp currentTime) {
		return this.market.clear(currentTime);
	}

	@Override
	public String toString() {
		return super.toString() + market;
	}
	
}
