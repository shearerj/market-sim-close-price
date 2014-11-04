package utils;

import java.util.Iterator;
import java.util.Random;

import logger.Log;

import com.google.common.base.Optional;

import data.FundamentalValue;
import data.Props;
import data.Stats;
import entity.agent.Agent;
import entity.agent.position.PrivateValues;
import entity.market.CDAMarket;
import entity.market.Market;
import entity.market.Price;
import entity.sip.BestBidAsk;
import entity.sip.MarketInfo;
import event.Activity;
import event.Timeline;
import event.TimeStamp;

public class Mock {
	
	private static final Iterator<Integer> ids = Iterators2.counter();
	private static final Random rand = new Random();
	private static final Optional<Market> absentMarket = Optional.absent();
	private static final Optional<Price> absentPrice = Optional.absent();

	public static Agent agent() {
		return agent(timeline);
	}
	
	public static Agent agent(Timeline timeline) {
		return new Agent(ids.next(), stats, timeline, Log.nullLogger(), rand, sip, fundamental,
				PrivateValues.zero(), TimeStamp.ZERO, Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
			@Override protected void agentStrategy() { }
		};
	}
	
	public static Market market() {
		return market(timeline);
	}
	
	public static Market market(Timeline timeline) {
		return CDAMarket.create(ids.next(), stats, timeline, Log.nullLogger(), rand, sip, Props.fromPairs());
	}
	
	public static final MockTimeLine timeline = new MockTimeLine() {
		private boolean ignoreNext = false;
		
		@Override public void scheduleActivityIn(TimeStamp delay, Activity act) {
			if (delay.equals(TimeStamp.ZERO) || delay.equals(TimeStamp.IMMEDIATE))
				if (ignoreNext)
					ignoreNext = false;
				else
					act.execute();
		}
		@Override public TimeStamp getCurrentTime() { return TimeStamp.ZERO; }
		@Override public void ignoreNext() { ignoreNext = true; }
	};
	
	public static final MarketInfo sip = new MarketInfo() {
		@Override public void processMarket(Market market) { }
		@Override public BestBidAsk getNBBO() {
			return BestBidAsk.create(absentMarket, absentPrice, 0, absentMarket, absentPrice, 0);
		}
	};
	
	public static Stats stats = new Stats() {
		@Override public void post(String name, double value) { }
		@Override public void post(String name, double value, long times) { }
		@Override public void postTimed(TimeStamp time, String name, double value) { }
	};
	
	public static FundamentalValue fundamental(int mean) {
		return FundamentalValue.create(stats, timeline, 0, mean, 0, rand);
	}
	
	public static final FundamentalValue fundamental = fundamental(0);
	
	public static interface MockTimeLine extends Timeline {
		public void ignoreNext();
	}
	
}
