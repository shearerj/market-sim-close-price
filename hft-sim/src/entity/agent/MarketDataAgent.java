package entity.agent;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import logger.Log;
import systemmanager.Consts;
import systemmanager.Keys.FileName;
import utils.Rand;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

import data.FundamentalValue;
import data.Props;
import data.Stats;
import entity.agent.MarketDataParser.MarketAction;
import entity.agent.position.PrivateValues;
import entity.market.Market;
import entity.market.Price;
import entity.sip.MarketInfo;
import event.TimeStamp;
import event.Timeline;
import fourheap.Order.OrderType;

public class MarketDataAgent extends SMAgent {
	protected final PeekingIterator<MarketAction> orderDatumIterator;
	protected final BiMap<Long, OrderRecord> refNumbers;

	protected MarketDataAgent(int id, Stats stats, Timeline timeline, Log log, Rand rand, MarketInfo sip, FundamentalValue fundamental,
			Market market, Iterator<MarketAction> orderDatumIterator, Props props) {
		super(id, stats, timeline, log, rand, sip, fundamental, PrivateValues.zero(), ImmutableList.<TimeStamp> of().iterator(),
				market, props);
		this.orderDatumIterator = Iterators.peekingIterator(checkNotNull(orderDatumIterator));
		refNumbers = HashBiMap.create();
		if (this.orderDatumIterator.hasNext())
			reenterIn(this.orderDatumIterator.peek().getScheduledTime());
	}
	
	public static MarketDataAgent create(int id, Stats stats, Timeline timeline, Log log, Rand rand, MarketInfo sip, FundamentalValue fundamental,
			Market market, Props props) {
		Iterator<MarketAction> actions = ImmutableList.<MarketAction> of().iterator();
		String fileName = props.get(FileName.class);
		
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
		
		return new MarketDataAgent(id, stats, timeline, log, rand, sip, fundamental, market, actions, props);
	}
	
	@Override
	protected void agentStrategy() {
		super.agentStrategy();
		
		if (!orderDatumIterator.hasNext())
			return;
		
		orderDatumIterator.next().executeFor(this);
		
		if (orderDatumIterator.hasNext())
			reenterIn(orderDatumIterator.peek().getScheduledTime().minus(getCurrentTime()));
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
	protected void orderRemoved(OrderRecord order, int removedQuantity) {
		super.orderRemoved(order, removedQuantity);
		if (order.getQuantity() == 0)
			refNumbers.inverse().remove(order);
	}

	private static final long serialVersionUID = 7690956351534734324L;

}
