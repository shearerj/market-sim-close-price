package data;

import java.util.NavigableMap;

import utils.SummStats;

import com.google.common.collect.Maps;

import event.TimeStamp;

/**
 * This class represents the summary of statistics after a run of the
 * simulation. The majority of the statistics that it collects are processed via
 * the EventBus, which is a message passing interface that can handle any style
 * of object. The rest, which mainly includes agent and player payoffs, is
 * processed when the call to getFeatures or getPlayerObservations is made.
 * 
 * Because this uses message passing, if you want the observation data structure
 * to get data, you must make sure to "register" it with the EventBus by calling
 * BUS.register(observations).
 * 
 * A single observation doesn't have that ability to save its results. Instead
 * you need to add it to a MultiSimulationObservation and use that class to
 * actually do the output.
 * 
 * To add statistics to the Observation:
 * <ol>
 * <li>Add the appropriate data structures to the object to record the
 * information you're interested in.</li>
 * <li>Create a listener method (located at the bottom, to tell what objects to
 * handle, and what to do with them.</li>
 * <li>Modify get features to take the aggregate data you stored, and turn it
 * into a String, double pair.</li>
 * <li>Wherever the relevant data is added simply add a line like
 * "Observations.BUS.post(dataObj);" with your appropriate data</li>
 * </ol>
 * 
 * @author erik
 * 
 */
public class Stats {
	
	// Different types of statistics
	private final NavigableMap<String, SummStats> summaryStats;
	private final NavigableMap<String, TimeSeries> timeStats;
	
	protected Stats() {
		summaryStats = Maps.newTreeMap();
		timeStats = Maps.newTreeMap();
	}
	
	public static Stats create() {
		return new Stats();
	}
	
	public NavigableMap<String, SummStats> getSummaryStats() {
		return Maps.unmodifiableNavigableMap(summaryStats);
	}
	
	public NavigableMap<String, TimeSeries> getTimeStats() {
		return Maps.unmodifiableNavigableMap(timeStats);
	}
	
	public void post(String name, double value) {
		SummStats summ = summaryStats.get(name);
		if (summ == null)
			summaryStats.put(name, SummStats.on(value));
		else
			summ.add(value);
	}
	
	// TODO Switch to default map?
	public void postTimed(TimeStamp time, String name, double value) {
		TimeSeries times = timeStats.get(name);
		if (times == null) {
			times = TimeSeries.create();
			timeStats.put(name, times);
		}
		times.add(time.getInTicks(), value);
	}
	
	public static final String
	FUNDAMENTAL = "fundamental_value",
	CONTROL_FUNDAMENTAL = "control_fund",
	CONTROL_PRIVATE_VALUE = "control_private",
	FUNDAMENTAL_END_PRICE = "fund_end_price",
	TRANSACTION_PRICE = "trans_prices",
	SPREAD = "spread_",
	MIDQUOTE = "midquote_",
	NBBO_SPREAD = SPREAD + "nbbo",
	EXECUTION_TIME = "execution_time",
	MARKET_MAKER_EXECTUION_TIME = "mm_exectime_",
	MARKET_MAKER_SPREAD = "mm_spreads_",
	MARKET_MAKER_LADDER = "mm_ladder_",
	PRICE = "price",
	NUM_TRANS = "num_trans_",
	NUM_TRANS_TOTAL = NUM_TRANS + "total",
	CLASS_PROFIT = "profit_",
	TOTAL_PROFIT = CLASS_PROFIT + "sum_total";
}
