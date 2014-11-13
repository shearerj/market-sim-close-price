package data;

import java.util.Collections;
import java.util.SortedMap;

import utils.Maps2;
import utils.SummStats;

import com.google.common.base.Supplier;
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
	private final SortedMap<String, SummStats> summaryStats;
	private final SortedMap<String, TimeSeries> timeStats;
	
	protected Stats() {
		summaryStats = Maps2.addDefault(Maps.<String, SummStats> newTreeMap(), new Supplier<SummStats>() {
			@Override public SummStats get() { return SummStats.on(); }
		});
		timeStats = Maps2.addDefault(Maps.<String, TimeSeries> newTreeMap(), new Supplier<TimeSeries>() {
			@Override public TimeSeries get() { return TimeSeries.create(); }
		});
	}
	
	public static Stats create() {
		return new Stats();
	}
	
	public SortedMap<String, SummStats> getSummaryStats() {
		return Collections.unmodifiableSortedMap(summaryStats);
	}
	
	public SortedMap<String, TimeSeries> getTimeStats() {
		return Collections.unmodifiableSortedMap(timeStats);
	}
	
	public void post(String name, double value) {
		summaryStats.get(name).add(value);
	}
	
	public void post(String name, double value, long times) {
		summaryStats.get(name).addNTimes(value, times);
	}
	
	public void postTimed(TimeStamp time, String name, double value) {
		timeStats.get(name).add(time.getInTicks(), value);
	}
	
	public static final String
	FUNDAMENTAL =			"fundamental_value",
	CONTROL_FUNDAMENTAL =	"control_fund",
	CONTROL_PRIVATE_VALUE =	"control_private",
	FUNDAMENTAL_END_PRICE =	"fund_end_price",
	TRANSACTION_PRICE =		"trans_prices",
	SPREAD =				"spread_",
	MIDQUOTE =				"midquote_",
	NBBO_SPREAD =			SPREAD + "nbbo",
	EXECUTION_TIME =		"execution_time",
	MARKET_MAKER_EXECTUION_TIME = "mm_exectime_",
	MARKET_MAKER_SPREAD =	"mm_spreads_",
	MARKET_MAKER_LADDER =	"mm_ladder_",
	MARKET_MAKER_EXEC =		"mm_rungs_exec",
	MARKET_MAKER_TRUNC =	"mm_rungs_trunc",
	PRICE =					"price",
	NUM_TRANS =				"num_trans_",
	NUM_TRANS_TOTAL =		NUM_TRANS + "total",
	PROFIT =				"profit_",
	TOTAL_PROFIT =			PROFIT + "sum_total",
	ZIRP_GREEDY =			"zirp_greedy",
	MAX_EFF_POSITION =		"position_",
	SURPLUS =				"surplus_";
}
