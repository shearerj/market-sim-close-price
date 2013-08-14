package activity;

import java.util.Collection;
import org.apache.commons.lang3.builder.*;

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
		this.market = market;
	}

	@Override
	public Collection<? extends Activity> execute(TimeStamp currentTime) {
		return this.market.clear(currentTime);
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder(19, 37).append(market).append(
				super.hashCode()).toHashCode();
	}

	@Override
	public String toString() {
		return super.toString() + market;
	}
	
}
