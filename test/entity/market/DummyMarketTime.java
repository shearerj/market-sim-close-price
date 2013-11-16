package entity.market;

import com.google.common.base.Objects;

import event.TimeStamp;

public class DummyMarketTime extends MarketTime {

	private static final long serialVersionUID = 1L;
	
	public DummyMarketTime(TimeStamp time, long marketTime) {
		super(time, marketTime);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		MarketTime other = (MarketTime) obj;
		return Objects.equal(time, other.time) && marketTime == other.marketTime;
	}
}
