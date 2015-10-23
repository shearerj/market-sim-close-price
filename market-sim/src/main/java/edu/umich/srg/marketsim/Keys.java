package edu.umich.srg.marketsim;

import edu.umich.srg.egtaonline.spec.ParsableValue.DoubleValue;
import edu.umich.srg.egtaonline.spec.ParsableValue.EnumValue;
import edu.umich.srg.egtaonline.spec.ParsableValue.IntValue;
import edu.umich.srg.egtaonline.spec.ParsableValue.LongValue;
import edu.umich.srg.egtaonline.spec.ParsableValue.StringsValue;
import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.fourheap.Order.OrderType;

/**
 * All of the keys for use in the simulation spec. Parameters must be camelCase.
 * OK for observation file descriptors to not be camel case.
 * 
 * @author erik
 * 
 */
public interface Keys {
	
	// Simulation
	public static class RandomSeed extends LongValue {};
	public static class SimLength extends LongValue {};
	public static class Markets extends StringsValue {};

	public static class FundamentalMean extends DoubleValue {};
	public static class FundamentalShockVar extends DoubleValue {};
	public static class FundamentalMeanReversion extends DoubleValue {};
	public static class FundamentalShockProb extends DoubleValue {};

	// Agents
	public static class ArrivalRate extends DoubleValue {};
	public static class FundamentalObservationVariance extends DoubleValue {};
	
	public static class Type extends EnumValue<OrderType> { public Type() { super(OrderType.class); }};
	public static class MaxPosition extends IntValue {};
	public static class PrivateValueVar extends DoubleValue {};
	public static class Rmax extends IntValue {};
	public static class Rmin extends IntValue {};
	
	public static class PriceVarianceEstimate extends DoubleValue {};
	
	public static class NumShockOrders extends IntValue {};
	
//	public static class DiscountFactors extends DoublesValue {};
//	
//	public static class FileName extends StringValue {};
//
//	public static class Window extends TimeValue {};
//	public static class Alpha extends DoubleValue {};
//	
//	public static class TickSize extends IntValue {};
//	
//	
//	public static class Num extends IntValue {};
//	public static class NumAgents extends IntValue {};
//	public static class NumMarkets extends IntValue {};
	
	// Latency
//	public static class NbboLatency extends TimeValue {};
//	public static class MarketLatency extends TimeValue {};
//	public static class LaLatency extends TimeValue {};
//	public static class FundamentalLatency extends TimeValue {};

	// Call Market
//	public static class ClearInterval extends TimeValue {};
//	public static class PricingPolicy extends DoubleValue {};
	
	// Agents
//	public static class Withdraw extends BoolValue {};
//	public static class N extends IntValue {};
	
//	public static class ReentryType extends EnumValue<Reentries> { public ReentryType() { super(Reentries.class); }};

	// Market Maker
//	public static class K extends IntValue {};
//	public static class Size extends IntValue {};
//	public static class Trunc extends BoolValue {};
//	public static class TickImprovement extends BoolValue {};
//	public static class TickOutside extends BoolValue {};
//	public static class InitLadderMean extends IntValue {};
//	public static class InitLadderRange extends IntValue {};

	// AAAgent
//	public static class Eta extends IntValue {};
//	public static class LambdaR extends DoubleValue {};
//	public static class LambdaA extends DoubleValue {};
//	public static class Gamma extends DoubleValue {};
//	public static class BetaR extends DoubleValue {};
//	public static class BetaT extends DoubleValue {};
//	public static class InitAggression extends DoubleValue {};
//	public static class Theta extends DoubleValue {};
//	public static class ThetaMax extends DoubleValue {};
//	public static class ThetaMin extends DoubleValue {};
//	public static class Debug extends BoolValue {};
//	public static class BuyerStatus extends EnumValue<OrderType> { public BuyerStatus() { super(OrderType.class); }};

	// ZIPAgent
//	public static class MarginMin extends DoubleValue {};
//	public static class MarginMax extends DoubleValue {};
//	public static class GammaMin extends DoubleValue {};
//	public static class GammaMax extends DoubleValue {};
//	public static class BetaMin extends DoubleValue {};
//	public static class BetaMax extends DoubleValue {};
//	public static class RangeR extends DoubleValue {};
//	public static class RangeA extends DoubleValue {};

	// Market Makers
//	public static class W extends DoubleValue {};
//	public static class Strats extends IntsValue {};
//	public static class UseMedianSpread extends BoolValue {};
//	public static class MovingAveragePrice extends BoolValue {};
//	public static class FastLearning extends BoolValue {};
//	public static class UseLastPrice extends BoolValue {};
//	public static class FundEstimate extends PriceValue {};
//	public static class Spread extends PriceValue {};

	// ZIRPAgent
//	public static class Thresh extends DoubleValue {};
	
	// Helper Classes
//	static class TimeValue extends ParsableValue<TimeStamp> {
//		protected TimeValue() {
//			super(new Converter<String, TimeStamp>() {
//				@Override protected String doBackward(TimeStamp time) { return Long.toString(time.get()); }
//				@Override protected TimeStamp doForward(String string) { return TimeStamp.of(Long.parseLong(string)); }
//			});
//		}
//	}
	
//	static class PriceValue extends ParsableValue<Price> {
//		protected PriceValue() {
//			super(new Converter<String, Price>() {
//				@Override protected String doBackward(Price price) { return Long.toString(price.longValue()); }
//				@Override protected Price doForward(String string) { return Price.of(Long.parseLong(string)); }
//			});
//		}
//	}
	
	public static final Spec DEFAULT_KEYS = Spec.builder()
			.put(RandomSeed.class, System.nanoTime())
			.put(SimLength.class, 10000l)
			
			.put(FundamentalMean.class, 1e8)
			.put(FundamentalMeanReversion.class, 1e-5)  // FIXME Set appropriately
			.put(FundamentalShockVar.class, 1e6)  // FIXME Set appropriately
			.put(FundamentalShockProb.class, 1d)
						
			.build();
	
}
