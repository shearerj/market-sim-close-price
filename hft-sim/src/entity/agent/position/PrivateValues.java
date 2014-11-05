package entity.agent.position;

import java.io.Serializable;

import entity.market.Price;
import fourheap.Order.OrderType;

public final class PrivateValues {

	public static PrivateValue zero() {
		return new ZeroPrivateValue();
	}

	private static class ZeroPrivateValue implements PrivateValue, Serializable {
		@Override
		public int getMaxAbsPosition() {
			return Integer.MAX_VALUE;
		}

		@Override
		public Price getValue(int currentPosition, OrderType type) {
			return Price.ZERO;
		}

		@Override
		public Price getValue(int currentPosition, int quantity,
				OrderType type) {
			return Price.ZERO;
		}

		@Override
		public Price getMean() {
			return Price.ZERO;
		}

		private static final long serialVersionUID = 6127006057342948602L;
	}
	
}
