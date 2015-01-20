package data;

import java.util.Collections;
import java.util.SortedMap;

import utils.Maps2;
import utils.SummStats;

import com.google.common.base.Supplier;
import com.google.common.collect.Maps;

import event.TimeStamp;

/**
 * This class represents all of the relevant statistics after a run of the
 * simulation.
 * 
 * There are two main types. Timed, which are stored in a time series, and
 * summary which are aggregated in a SummStats object. Both types are handeled
 * differently when ultimately incorporated into an observation.
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
		summaryStats.get(name).addN(value, times);
	}
	
	public void postTimed(TimeStamp time, String name, double value) {
		timeStats.get(name).add(time, value);
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
	MARKET_MAKER_EXECUTION_TIME = "mm_exectime_",
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
