package systemmanager;

import com.google.common.base.Converter;

import event.TimeStamp;
import fourheap.Order.OrderType;
import props.ParsableValue;
import props.ParsableValue.BoolValue;
import props.ParsableValue.DoubleValue;
import props.ParsableValue.EnumValue;
import props.ParsableValue.IntValue;
import props.ParsableValue.IntsValue;
import props.ParsableValue.LongValue;
import props.ParsableValue.StringValue;

/**
 * All of the keys for use in the simulation spec. Parameters be camelCase.
 * OK for observation file descriptors to not be camel case.
 * 
 * @author erik
 * 
 */
public interface Keys {

	public static class RandomSeed extends LongValue {};
	public static class NumSims extends IntValue {};
	
	public static class FileName extends StringValue {};
	
	public static class ArrivalRate extends DoubleValue {};
	
	public static class ReentryRate extends DoubleValue {};
	public static class BackgroundReentryRate extends DoubleValue {};
	public static class MarketMakerReentryRate extends DoubleValue {};

	public static class WindowLength extends TimeValue {};
	public static class BidRangeMax extends IntValue {};
	public static class BidRangeMin extends IntValue {};
	public static class MaxQty extends IntValue {};
	public static class Alpha extends DoubleValue {};
	
	public static class TickSize extends IntValue {};
	public static class MarketTickSize extends IntValue {};
	public static class AgentTickSize extends IntValue {};
	
	public static class PrivateValueVar extends DoubleValue {};
	public static class SimLength extends IntValue {};
	public static class FundamentalMean extends IntValue {};
	public static class FundamentalShockVar extends DoubleValue {};
	public static class FundamentalKappa extends DoubleValue {};
	
	public static class Num extends IntValue {};
	public static class NumAgents extends IntValue {};
	public static class NumMarkets extends IntValue {};
	
	// Latency
	public static class NbboLatency extends TimeValue {};
	public static class MarketLatency extends TimeValue {};
	public static class LaLatency extends TimeValue {};
	public static class FundamentalLatency extends TimeValue {};

	// Call Market
	public static class ClearFrequency extends TimeValue {};
	public static class PricingPolicy extends DoubleValue {};
	
	// Agents
	public static class WithdrawOrders extends BoolValue {};
	public static class NumHistorical extends IntValue {};

	// Market Maker
	public static class NumRungs extends IntValue {};
	public static class RungSize extends IntValue {};
	public static class TruncateLadder extends BoolValue {};
	public static class TickImprovement extends BoolValue {};
	public static class TickOutside extends BoolValue {};
	public static class InitLadderMean extends IntValue {};
	public static class InitLadderRange extends IntValue {};

	// AAAgent
	public static class Eta extends IntValue {};
	public static class LambdaR extends DoubleValue {};
	public static class LambdaA extends DoubleValue {};
	public static class Gamma extends DoubleValue {};
	public static class BetaR extends DoubleValue {};
	public static class BetaT extends DoubleValue {};
	public static class InitAggression extends DoubleValue {};
	public static class Theta extends DoubleValue {};
	public static class ThetaMax extends DoubleValue {};
	public static class ThetaMin extends DoubleValue {};
	public static class Debug extends BoolValue {};
	public static class BuyerStatus extends EnumValue<OrderType> { public BuyerStatus() { super(OrderType.class); }};

	// ZIPAgent
	public static class MarginMin extends DoubleValue {};
	public static class MarginMax extends DoubleValue {};
	public static class GammaMin extends DoubleValue {};
	public static class GammaMax extends DoubleValue {};
	public static class BetaMin extends DoubleValue {};
	public static class BetaMax extends DoubleValue {};
	public static class RangeR extends DoubleValue {};
	public static class RangeA extends DoubleValue {};

	// Market Makers
	public static class WeightFactor extends DoubleValue {};
	public static class Spreads extends IntsValue {};
	public static class UseMedianSpread extends BoolValue {};
	public static class MovingAveragePrice extends BoolValue {};
	public static class FastLearning extends BoolValue {};
	public static class UseLastPrice extends BoolValue {};
	public static class FundEstimate extends LongValue {};

	// ZIRPAgent
	public static class AcceptableProfitFrac extends DoubleValue {};
	
	// Helper Classes
	static class TimeValue extends ParsableValue<TimeStamp> {
		protected TimeValue() {
			super(new Converter<String, TimeStamp>() {
				@Override protected String doBackward(TimeStamp time) { return Long.toString(time.getInTicks()); }
				@Override protected TimeStamp doForward(String string) { return TimeStamp.of(Long.parseLong(string)); }
			});
		}
		
	}
}
