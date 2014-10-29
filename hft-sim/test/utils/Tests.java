package utils;

import static fourheap.Order.OrderType.BUY;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import junit.framework.AssertionFailedError;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;

import entity.agent.OrderRecord;
import entity.market.Market;
import entity.market.Price;
import entity.market.Quote;
import entity.market.Transaction;
import entity.sip.BestBidAsk;
import event.TimeStamp;
import fourheap.Order.OrderType;

public abstract class Tests {
	
	public static Joiner j = Joiner.on('_');

	// FIXME Remove unused methods / methods that start with check
	
	/** Prices are null for absent prices (for convenience) */
	public static void assertQuote(Quote quote, Price bid, int bidQuantity, Price ask, int askQuantity) {
		if (ask == null)
			assertEquals(0, askQuantity);
		if (bid == null)
			assertEquals(0, bidQuantity);
		assertEquals("Incorrect ASK", Optional.fromNullable(ask),  quote.getAskPrice());
		assertEquals("Incorrect BID", Optional.fromNullable(bid),  quote.getBidPrice());
		assertEquals("Incorrect ASK quantity",  askQuantity,  quote.getAskQuantity());
		assertEquals("Incorrect BID quantity",  bidQuantity,  quote.getBidQuantity());
	}
	
	public static void assertSingleTransaction(Collection<Transaction> singleTransaction, Price price, TimeStamp time, int quantity) {
		assertEquals("Collection didn't have a single transaction", 1, singleTransaction.size());
		checkTransaction(Iterables.getOnlyElement(singleTransaction), price, time, quantity);
	}
	
	public static void checkTransaction(Transaction transaction, Price price, TimeStamp time, int quantity) {
		assertEquals("Incorrect Price", price, transaction.getPrice());
		assertEquals("Incorrect Time", time, transaction.getExecTime());
		assertEquals("Incorrect Quantity", quantity, transaction.getQuantity());
	}
	
	public static void checkSingleOrder(Collection<OrderRecord> singleOrder, Price price, int quantity, TimeStamp createdTime, TimeStamp submittedTime) {
		assertEquals("Collection didn't have a single order record", 1, singleOrder.size());
		assertOrder(Iterables.getOnlyElement(singleOrder), price, quantity, createdTime, submittedTime);
	}
	
	public static void checkSingleOrder(Collection<OrderRecord> singleOrder, OrderType type, Price price, int quantity, TimeStamp createdTime, TimeStamp submittedTime) {
		assertEquals("Collection didn't have a single order record", 1, singleOrder.size());
		checkOrder(Iterables.getOnlyElement(singleOrder), type, price, quantity, createdTime, submittedTime);
	}
	
	/** Absent submitted time is null for convenience */
	public static void assertOrder(OrderRecord order, Price price, int quantity, TimeStamp createdTime, TimeStamp submittedTime) {
		assertEquals("Incorrect Price", price, order.getPrice());
		assertEquals("Incorrect Quantity", quantity, order.getQuantity());
		assertEquals("Incorrect Created Time", createdTime, order.getCreatedTime());
		assertEquals("Incorrect Submitted Time", Optional.fromNullable(submittedTime), order.getSubmitTime());
	}
	
	public static void checkOrder(OrderRecord order, OrderType type, Price price, int quantity, TimeStamp createdTime, TimeStamp submittedTime) {
		assertEquals("Incorrect Order Type", type, order.getOrderType());
		assertOrder(order, price, quantity, createdTime, submittedTime);
	}
	
	public static void assertSingleOrderRange(Collection<OrderRecord> singleOrder, Price low, Price high, int quantity) {
		assertEquals("Collection didn't have a single order record", 1, singleOrder.size());
		checkOrderRange(Iterables.getOnlyElement(singleOrder), low, high, quantity);
	}
	
	public static void checkOrderRange(OrderRecord order, Price low, Price high, int quantity) {
		Range<Price> bounds = Range.closed(low, high);
		assertTrue(bounds.contains(order.getPrice()), "Order price (%s) out of bounds %s", order.getPrice(), bounds);
		assertEquals(quantity, order.getQuantity());
	}
	
	public static void assertNBBO(BestBidAsk nbbo, Price bid, Market bidMarket, int bidQuantity,
			Price ask, Market askMarket, int askQuantity) {
		assertNBBO(nbbo, bid, bidMarket, ask, askMarket);
		assertEquals("Inccorect ASK quantity", askQuantity, nbbo.getBestAskQuantity());
		assertEquals("Inccorect BID quantity", bidQuantity, nbbo.getBestBidQuantity());
	}
	
	public static void assertNBBO(BestBidAsk nbbo, Price bid, Market bidMarket, Price ask, Market askMarket) {
		assertEquals("Incorrect ASK", Optional.fromNullable(ask), nbbo.getBestAsk());
		assertEquals("Incorrect BID", Optional.fromNullable(bid), nbbo.getBestBid());
		assertEquals("Incorrect ASK market", Optional.fromNullable(askMarket), nbbo.getBestAskMarket());
		assertEquals("Incorrect BID market", Optional.fromNullable(bidMarket), nbbo.getBestBidMarket());
	}
	
	public static void assertOrderLadder(Collection<OrderRecord> orders, Price... prices) {
		assertOrderLadder(orders, prices.length / 2, prices);
	}
	
	/** First half are buy prices, second half are sell prices */
	public static void assertOrderLadder(Collection<OrderRecord> orders, int numBuys, Price... prices) {
		assertEquals("Incorrect number of orders", prices.length, orders.size());
		
		Set<Price> buys = Sets.newHashSet(Arrays.asList(prices).subList(0, numBuys));
		Set<Price> sells = Sets.newHashSet(Arrays.asList(prices).subList(numBuys, prices.length));
		
		for (OrderRecord order : orders) {
			if (order.getOrderType() == BUY)
				assertTrue(buys.remove(order.getPrice()), "Placed unexpected buy order at %s", order.getPrice());
			else
				assertTrue(sells.remove(order.getPrice()), "Placed unexpected sell order at %s", order.getPrice());
		}
		assertTrue(buys.isEmpty(), "Didn't place %s buys", buys);
		assertTrue(sells.isEmpty(), "Didn't place %s sells", sells);
	}
	
	// FIXME, this test isn't quite right, as it it doesn't also enforce that ask - bid = 2 * rungSize
	public static void assertRandomOrderLadder(Collection<OrderRecord> orders, int size,
			Range<Price> ladderCenterRange, int rungSize) {
		Range<Price> bidRange = Range.closed(
				Price.of(ladderCenterRange.lowerEndpoint().intValue() - 5),
				Price.of(ladderCenterRange.upperEndpoint().intValue() - 5));
		Range<Price> askRange = Range.closed(
				Price.of(ladderCenterRange.lowerEndpoint().intValue() + 5),
				Price.of(ladderCenterRange.upperEndpoint().intValue() + 5));
		assertRandomOrderLadder(orders, size, bidRange, askRange, rungSize);
	}
	
	public static void assertRandomOrderLadder(Collection<OrderRecord> orders, int size,
			Range<Price> bidRange, Range<Price> askRange, int rungSize) {
		assertEquals("Incorrect number of orders", size, orders.size());
		List<Price> buys = Lists.newArrayList();
		List<Price> sells = Lists.newArrayList();
		for (OrderRecord order : orders)
			(order.getOrderType() == BUY ? buys : sells).add(order.getPrice());
		Collections.sort(buys, Ordering.natural().reverse());
		Collections.sort(sells, Ordering.natural());
		
		assertTrue(bidRange.contains(buys.get(0)), "Ladder bid (%s) is outside of range %s", buys.get(0), bidRange);
		assertTrue(askRange.contains(sells.get(0)), "Ladder ask (%s) is outside of range %s", sells.get(0), askRange);
		
		int buyPrice = buys.get(0).intValue();
		for (Price buy : Iterables.skip(buys, 1))
			assertEquals("Incorrect Price", Price.of(buyPrice -= rungSize), buy);
		int sellPrice = sells.get(0).intValue();
		for (Price sell : Iterables.skip(sells, 1))
			assertEquals("Incorrect Price", Price.of(sellPrice += rungSize), sell);
	}

	public static void assertTrue(boolean check, String format, Object... parameters) {
		if (!check)
			throw new AssertionFailedError(String.format(format, parameters));
	}
	
	public static void assertRegex(String regex, String actual) {
		assertTrue(actual.matches(regex), "String \"%s\" didn't match regex \"%s\"", actual, regex);
	}
	
	public static <T extends Comparable<T>> void assertRange(T value, T lower, T upper) {
		Range<T> range = Range.closed(lower, upper);
		assertTrue(range.contains(value), "Value %s is out of bounds %s", value, range); 
	}
	
	public static <T extends Comparable<T>> void assertOptionalRange(Optional<T> value, T lower, T upper) {
		assertTrue(value.isPresent(), "Value is not present");
		assertRange(value.get(), lower, upper); 
	}
	
}
