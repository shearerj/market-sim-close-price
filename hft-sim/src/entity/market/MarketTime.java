package entity.market;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;

import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;

import event.TimeStamp;

public class MarketTime implements Comparable<MarketTime>, Serializable {

	public static final MarketTime ZERO = MarketTime.from(TimeStamp.ZERO, 0);
	
	private final TimeStamp time;
	private final long marketTime;

	protected MarketTime(TimeStamp time, long marketTime) {
		this.time = checkNotNull(time);
		this.marketTime = marketTime;
	}
	
	public static MarketTime from(TimeStamp time, long marketTime) {
		return new MarketTime(time, marketTime);
	}
	
	public TimeStamp getTime() {
		return time;
	}
	
	@Override
	public int compareTo(MarketTime that) {
		return ComparisonChain.start().compare(this.time, that.time).compare(this.marketTime, that.marketTime).result();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof MarketTime))
			return false;
		MarketTime that = (MarketTime) obj;
		return Objects.equal(this.time, that.time) && this.marketTime == that.marketTime;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(super.hashCode(), marketTime);
	}

	private static final long serialVersionUID = 2228639267105816484L;

}
