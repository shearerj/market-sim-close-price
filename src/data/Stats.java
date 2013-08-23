package data;

import java.util.Map;

import logger.Logger;

import systemmanager.EventManager;

import com.google.common.collect.Maps;
import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import entity.market.Market;

// TODO I think this should maybe be how observations works, but I can decided if they should be stand alone or not... 
public class Stats {

	public static final EventBus BUS = new EventBus();
	
	protected final EventManager manager;
	protected final Map<Market, TimeSeries> spreads; 

	public Stats(EventManager manager) {
		this.manager = manager;
		this.spreads = Maps.newHashMap();
		
		BUS.register(this);
	}
	
	@Subscribe public void processSpreads(SpreadStat s) {
		Market market = s.owner;
		TimeSeries series = spreads.get(market);
		if (series == null) {
			series = new TimeSeries();
			spreads.put(market, series);
		}
		series.add(manager.getCurrentTime().getInTicks(), s.val);
	}
	
	@Subscribe public void deadStat(DeadEvent d) {
		Logger.log(Logger.Level.ERROR, "Unhandled Statistic: " + d);
	}
	
	public static class Stat<O, V> {
		protected final V val;
		protected final O owner;

		public Stat(O owner, V val) {
			this.owner = owner;
			this.val = val;
		}
	}
	
	public static class SpreadStat extends Stat<Market, Double> {
		public SpreadStat(Market market, double val) { super(market, val); }
	}
	
}
