package entity.market;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;

import com.google.common.base.Objects;
import com.google.common.primitives.Longs;

import event.TimeStamp;

public class MarketTime implements Comparable<MarketTime>, Serializable {

	private static final long serialVersionUID = 2228639267105816484L;
	
	protected final TimeStamp time;
	protected final long marketTime;

	protected MarketTime(TimeStamp time, long marketTime) {
		this.time = checkNotNull(time);
		this.marketTime = marketTime;
	}
	
	public TimeStamp getTime() {
		return time;
	}
	
	@Override
	public int compareTo(MarketTime o) {
		return Integer.signum(time.compareTo(time)) * 2
				+ Integer.signum(Longs.compare(marketTime, o.marketTime));
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !obj.getClass().equals(MarketTime.class)) return false;
		MarketTime other = (MarketTime) obj;
		return Objects.equal(time, other.time) && marketTime == other.marketTime;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(time, marketTime);
	}

	@Override
	public String toString() {
		return time.toString();
	}

}
