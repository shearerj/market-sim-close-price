package entity.market;

import java.io.Serializable;

import com.google.common.base.Objects;
import com.google.common.primitives.Longs;

import event.TimeStamp;

public class MarketTime extends TimeStamp implements Serializable {

	private static final long serialVersionUID = 2228639267105816484L;
	
	protected final long marketTime;

	protected MarketTime(TimeStamp time, long marketTime) {
		super(time);
		this.marketTime = marketTime;
	}
	
	@Override
	public int compareTo(TimeStamp other) {
		if (!(other instanceof MarketTime))
			return super.compareTo(other);
		MarketTime obj = (MarketTime) other;
		return super.compareTo(obj) == 0 ? Longs.compare(marketTime, obj.marketTime) : super.compareTo(obj);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !obj.getClass().equals(MarketTime.class)) return false;
		MarketTime other = (MarketTime) obj;
		return super.equals(obj) && marketTime == other.marketTime;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(super.hashCode(), marketTime);
	}

}
