package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.log;
import static logger.Log.Level.INFO;

import iterators.ExpInterarrivals;

import java.util.Iterator;
import java.util.Random;

import systemmanager.Keys;
import systemmanager.Scheduler;
import data.EntityProperties;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import event.TimeStamp;
import fourheap.Order.OrderType;

public final class ZIRPAgent extends BackgroundAgent {
	
	private static final long serialVersionUID = -8805640643365079141L;
	
	private final int simulationLength;
	private final double fundamentalKappa;
	private final double fundamentalMean;
	private final boolean withdrawOrders;
	
	private static final double DEFAULT_REENTRY_RATE = 0.005;
	private static final double DEFAULT_PRIVATE_VALUE = 100000000;
	private static final int DEFAULT_MAX_QUANTITY = 10;
	private static final int DEFAULT_BID_RANGE_MAX = 5000;
	
	public ZIRPAgent(
		final Scheduler scheduler, 
		final TimeStamp arrivalTime,
		final FundamentalValue fundamental, 
		final SIP sip, 
		final Market market, 
		final Random rand,
		final EntityProperties props
	) {
		this(
			scheduler, 
			arrivalTime, 
			fundamental, 
			sip, 
			market, 
			rand,
			ExpInterarrivals.create(
				props.getAsDouble(Keys.REENTRY_RATE, DEFAULT_REENTRY_RATE), 
				rand
			),
			props.getAsDouble(Keys.PRIVATE_VALUE_VAR, DEFAULT_PRIVATE_VALUE),
			props.getAsInt(Keys.TICK_SIZE, 1),
			props.getAsInt(Keys.MAX_QUANTITY, DEFAULT_MAX_QUANTITY),
			props.getAsInt(Keys.BID_RANGE_MIN, 0),
			props.getAsInt(Keys.BID_RANGE_MAX, DEFAULT_BID_RANGE_MAX),
			props.getAsBoolean(Keys.WITHDRAW_ORDERS, true),
			props.getAsInt(Keys.SIMULATION_LENGTH),
			props.getAsDouble(Keys.FUNDAMENTAL_KAPPA),
			props.getAsDouble(Keys.FUNDAMENTAL_MEAN));
		
	}

	private ZIRPAgent(
		final Scheduler scheduler, 
		final TimeStamp arrivalTime,
		final FundamentalValue fundamental, 
		final SIP sip,
		final Market market, 
		final Random rand,
		final Iterator<TimeStamp> interarrivals, 
		final double pvVar, 
		final int tickSize,
		final int maxAbsPosition, 
		final int bidRangeMin, 
		final int bidRangeMax,
		final boolean aWithdrawOrders,
		final int aSimulationLength, 
		final double aFundamentalKappa,
		final double aFundamentalMean) {
		
		super(scheduler, arrivalTime, fundamental, sip, market, rand,
				interarrivals, new PrivateValue(maxAbsPosition, pvVar, rand),
				tickSize, bidRangeMin, bidRangeMax);
		
		simulationLength = aSimulationLength;
		fundamentalKappa = aFundamentalKappa;
		fundamentalMean = aFundamentalMean;
		withdrawOrders = aWithdrawOrders;
	}

	@Override
	public void agentStrategy(final TimeStamp currentTime) {
		super.agentStrategy(currentTime);

		if (!currentTime.equals(arrivalTime)) {
			log.log(INFO, "%s wake up.", this);
		}

		if (withdrawOrders) {
			log.log(INFO, "%s Withdraw all orders.", this);
			withdrawAllOrders();
		}
		
		// 50% chance of being either long or short
		OrderType orderType = BUY;
		if (rand.nextBoolean()) {
			orderType = SELL;
		}
		log.log(INFO, "%s Submit %s order", this, orderType);
		executeZIRPStrategy(
			orderType, 
			1, 
			currentTime, 
			simulationLength, 
			fundamentalKappa, 
			fundamentalMean
		);
	}
}
