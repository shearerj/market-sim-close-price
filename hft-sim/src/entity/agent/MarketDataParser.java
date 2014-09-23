package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;

import java.io.Reader;
import java.util.Iterator;

import utils.Iterators2;

import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterators;

import entity.market.Price;
import event.TimeStamp;
import fourheap.Order.OrderType;

abstract class MarketDataParser {

	protected static final Splitter split = Splitter.on(',');

	public static Iterator<MarketAction> parseNYSE(Reader reader) {
		final Iterator<String> lines = Iterators2.lineIterator(reader);
		return Iterators.filter(new Iterator<MarketAction>() {
			
			@Override
			public boolean hasNext() {
				return lines.hasNext();
			}

			@Override
			public MarketAction next() {
				Iterator<String> elements = split.split(lines.next()).iterator();
				switch (elements.next().charAt(0)) {
				case 'A':
					return parseNYSEAdd(elements);
				case 'D':
					return parseNYSEDelete(elements);
				default: // Flags we don't handle
					return null;
				}
			}

			@Override
			public void remove() {
				lines.remove();
			}
		}, Predicates.not(Predicates.isNull()));
	}

	protected static SubmitOrder parseNYSEAdd(Iterator<String> elements) {
		// Need to check number of columns
		elements.next(); // Unused
		long orderReferenceNumber = Long.parseLong(elements.next());
		elements.next(); // Unused
		OrderType type = (elements.next().charAt(0) == 'B') ? BUY : SELL;
		int quantity = Integer.parseInt(elements.next());
		elements.next(); // Unused
		Price price = Price.of(Double.parseDouble(elements.next()));
		long seconds = Long.parseLong(elements.next());
		long milliseconds = Long.parseLong(elements.next());
		TimeStamp timestamp = TimeStamp.of(seconds*1000 + milliseconds);
		
		return new SubmitOrder(orderReferenceNumber, timestamp, price, quantity, type);
	}

	protected static WithdrawOrder parseNYSEDelete(Iterator<String> elements) {
		elements.next(); // Unused
		long orderRefNum = Long.parseLong(elements.next());
		long seconds = Long.parseLong(elements.next());
		long milliseconds = Long.parseLong(elements.next());
		
		return new WithdrawOrder(orderRefNum, TimeStamp.of(seconds * 1000 + milliseconds));
	}
	
	public static Iterator<MarketAction> parseNasdaq(Reader reader) {
		final Iterator<String> lines = Iterators2.lineIterator(reader);
		return Iterators.filter(new Iterator<MarketAction>() {
			private long seconds = 0;
			
			@Override
			public boolean hasNext() {
				return lines.hasNext();
			}

			@Override
			public MarketAction next() {
				Iterator<String> elements = split.split(lines.next()).iterator();
				switch (elements.next().charAt(0)) {
				case 'T':
					seconds = Long.parseLong(elements.next());
					return null;
				case 'A':
					return parseNasdaqAdd(seconds, elements);
				case 'D':
					return parseNasdaqDelete(seconds, elements);
				default: // Flags we don't handle
					return null;
				}
			}

			@Override
			public void remove() {
				lines.remove();
			}
		}, Predicates.not(Predicates.isNull()));
	}
	
	protected static SubmitOrder parseNasdaqAdd(long seconds, Iterator<String> elements) {
		TimeStamp timeStamp = TimeStamp.of(seconds + Long.parseLong(elements.next()) / 1000000);
		long orderRefNumber = Long.parseLong(elements.next());
		OrderType orderType = (elements.next().charAt(0) == 'B') ? OrderType.BUY : OrderType.SELL;
		int quantity = Integer.parseInt(elements.next());
		elements.next(); // Unused
		Price price = Price.of(Long.parseLong(elements.next()));
		
		return new SubmitOrder(orderRefNumber, timeStamp, price, quantity, orderType);
	}
	
	protected static WithdrawOrder parseNasdaqDelete(long seconds, Iterator<String> elements) {
		TimeStamp deleteTime = TimeStamp.of(seconds + Long.parseLong(elements.next()) / 1000000);
		long orderRefNum = Long.parseLong(elements.next());
		
		return new WithdrawOrder(orderRefNum, deleteTime);
	}
	
	interface MarketAction {
		
		TimeStamp getScheduledTime();
		
		void executeFor(MarketDataAgent agent);
		
	}
	
	static class SubmitOrder implements MarketAction {
		private final long orderRefNum;
		private final TimeStamp scheduledTime;
		private final Price price;
		private final int quantity;
		private final OrderType orderType;
		
		SubmitOrder(long orderRefNum, TimeStamp scheduledTime, Price price, int quantity, OrderType orderType) {
			this.orderRefNum = orderRefNum;
			this.scheduledTime = scheduledTime;
			this.price = price;
			this.quantity = quantity;
			this.orderType = orderType;
		}
		
		@Override
		public TimeStamp getScheduledTime() {
			return scheduledTime;
		}

		@Override
		public void executeFor(MarketDataAgent agent) {
			agent.submitRefOrder(orderType, price, quantity, orderRefNum);
		}
		
	}
	
	static class WithdrawOrder implements MarketAction {
		private final long orderRefNum;
		private final TimeStamp scheduledTime;
		
		WithdrawOrder(long orderRefNum, TimeStamp scheduledTime) {
			this.orderRefNum = orderRefNum;
			this.scheduledTime = scheduledTime;
		}

		@Override
		public TimeStamp getScheduledTime() {
			return scheduledTime;
		}

		@Override
		public void executeFor(MarketDataAgent agent) {
			OrderRecord order = agent.refNumbers.get(orderRefNum);
			if (order != null)
				agent.withdrawOrder(order);
		}
		
	}
	
}
