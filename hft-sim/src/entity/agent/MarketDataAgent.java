package entity.agent;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Random;

import systemmanager.Consts;
import systemmanager.Keys;
import systemmanager.Simulation;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

import data.Props;
import entity.agent.MarketDataParser.MarketAction;
import entity.market.Market;
import entity.market.Price;
import event.TimeStamp;
import fourheap.Order.OrderType;

public class MarketDataAgent extends SMAgent {
	protected final PeekingIterator<MarketAction> orderDatumIterator;
	protected final BiMap<Long, OrderRecord> refNumbers;

	protected MarketDataAgent(Simulation sim, TimeStamp arrivalTime, Market market, 
			Random rand, Iterator<MarketAction> orderDatumIterator, Props props) {
		super(sim, arrivalTime, rand, market, props);
		this.orderDatumIterator = Iterators.peekingIterator(checkNotNull(orderDatumIterator));
		refNumbers = HashBiMap.create();
	}
	
	public static MarketDataAgent create(Simulation sim, Market market, Random rand, Props props) {
		Iterator<MarketAction> actions = ImmutableList.<MarketAction> of().iterator();
		String fileName = props.getAsString(Keys.FILENAME);
		
		FileReader reader = null;
		try {
			reader = new FileReader(new File(fileName));
			
			if(fileName.toLowerCase().contains(Consts.NYSE))
				actions = MarketDataParser.parseNYSE(reader);
			else if(fileName.toLowerCase().contains(Consts.NASDAQ))
				actions = MarketDataParser.parseNasdaq(reader);
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (reader != null)
					reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		PeekingIterator<MarketAction> peekable = Iterators.peekingIterator(actions);
		TimeStamp arrivalTime = peekable.hasNext() ? peekable.peek().getScheduledTime() : TimeStamp.ZERO;
		return new MarketDataAgent(sim, arrivalTime, market, rand, peekable, props);
	}
	
	public void agentStrategy() {
		if (!orderDatumIterator.hasNext())
			return;
		
		orderDatumIterator.next().executeFor(this);
		
		if (orderDatumIterator.hasNext())
			reenterIn(orderDatumIterator.peek().getScheduledTime().minus(currentTime()));
	}
	
	protected OrderRecord submitRefOrder(OrderType type, Price price, int quantity, long refNum) {
		OrderRecord order = submitOrder(type, price, quantity);
		refNumbers.put(refNum, order);
		return order;
	}
	
	@Override
	protected void withdrawOrder(OrderRecord order) {
		super.withdrawOrder(order);
		refNumbers.inverse().remove(order);
	}

	@Override
	protected void orderTransacted(OrderRecord order, int removedQuantity) {
		super.orderTransacted(order, removedQuantity);
		if (order.getQuantity() == 0)
			refNumbers.inverse().remove(order);
	}

	private static final long serialVersionUID = 7690956351534734324L;

}
