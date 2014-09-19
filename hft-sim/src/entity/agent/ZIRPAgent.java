package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.log;
import static logger.Log.Level.INFO;

import java.util.Random;

import systemmanager.Keys;
import systemmanager.Scheduler;
import data.EntityProperties;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import event.TimeStamp;
import fourheap.Order.OrderType;

public class ZIRPAgent extends ZIRAgent {
	
	private static final long serialVersionUID = -8805640643365079141L;
	
	public ZIRPAgent(Scheduler scheduler, TimeStamp arrivalTime,
		FundamentalValue fundamental, SIP sip, Market market, Random rand,
		EntityProperties props) {
	
		this(scheduler, arrivalTime, fundamental, sip, market, rand,
			props.getAsDouble(Keys.REENTRY_RATE, 0.005),
			props.getAsDouble(Keys.PRIVATE_VALUE_VAR, 100000000),
			props.getAsInt(Keys.TICK_SIZE, 1),
			props.getAsInt(Keys.MAX_QUANTITY, 10),
			props.getAsInt(Keys.BID_RANGE_MIN, 0),
			props.getAsInt(Keys.BID_RANGE_MAX, 5000),
			props.getAsBoolean(Keys.WITHDRAW_ORDERS, true));
	}

	public ZIRPAgent(Scheduler scheduler, TimeStamp arrivalTime,
			FundamentalValue fundamental, SIP sip, Market market, Random rand,
			double reentryRate, double pvVar, int tickSize, int maxAbsPosition,
			int bidRangeMin, int bidRangeMax, boolean withdrawOrders) {
		super(scheduler, arrivalTime, fundamental, sip, market, rand, reentryRate,
				pvVar, tickSize, maxAbsPosition, bidRangeMin, bidRangeMax,
				withdrawOrders);
	}

	@Override
	public void agentStrategy(TimeStamp currentTime) {
		super.agentStrategy(currentTime);

		if (!currentTime.equals(arrivalTime)) {
			log.log(INFO, "%s wake up.", this);
		}

		if (withdrawOrders) {
			log.log(INFO, "%s Withdraw all orders.", this);
			withdrawAllOrders();
		}
		
		// 0.50% chance of being either long or short
		OrderType type = rand.nextBoolean() ? BUY : SELL;
		log.log(INFO, "%s Submit %s order", this, type);
		executeZIRPStrategy(type, 1, currentTime);
	}
}
