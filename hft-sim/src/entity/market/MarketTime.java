package entity.market;

import java.io.Serializable;

import com.google.common.base.Objects;
import com.google.common.primitives.Longs;

import event.TimeStamp;

public class MarketTime extends TimeStamp implements Serializable {

	private static final long serialVersionUID = 2228639267105816484L;
	
	private final long marketTime;

	protected MarketTime(TimeStamp time, long marketTime) {
		super(time.getInTicks());
		this.marketTime = marketTime;
	}
	
	protected static MarketTime from(TimeStamp time, long marketTime) {
		return new MarketTime(time, marketTime);
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
		if (obj == null || !(obj instanceof TimeStamp)) return false;
		if (obj instanceof MarketTime)
			return super.equals(obj) && marketTime == ((MarketTime) obj).marketTime;
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(super.hashCode(), marketTime);
	}

}
