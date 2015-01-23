package systemmanager;

import java.io.Serializable;

import props.ImmutableProps;
import props.Value;
import systemmanager.Keys.AcceptableProfitThreshold;
import systemmanager.Keys.Alpha;
import systemmanager.Keys.BetaMax;
import systemmanager.Keys.BetaMin;
import systemmanager.Keys.BetaR;
import systemmanager.Keys.BetaT;
import systemmanager.Keys.BuyerStatus;
import systemmanager.Keys.ClearInterval;
import systemmanager.Keys.Debug;
import systemmanager.Keys.DiscountFactors;
import systemmanager.Keys.Eta;
import systemmanager.Keys.FastLearning;
import systemmanager.Keys.FeatureWhitelist;
import systemmanager.Keys.FundEstimate;
import systemmanager.Keys.FundamentalKappa;
import systemmanager.Keys.FundamentalLatency;
import systemmanager.Keys.FundamentalMean;
import systemmanager.Keys.FundamentalShockVar;
import systemmanager.Keys.Gamma;
import systemmanager.Keys.GammaMax;
import systemmanager.Keys.GammaMin;
import systemmanager.Keys.InitAggression;
import systemmanager.Keys.InitLadderRange;
import systemmanager.Keys.K;
import systemmanager.Keys.LaLatency;
import systemmanager.Keys.LambdaA;
import systemmanager.Keys.LambdaR;
import systemmanager.Keys.MarginMax;
import systemmanager.Keys.MarginMin;
import systemmanager.Keys.MarketLatency;
import systemmanager.Keys.MaxPosition;
import systemmanager.Keys.MovingAveragePrice;
import systemmanager.Keys.N;
import systemmanager.Keys.NbboLatency;
import systemmanager.Keys.Num;
import systemmanager.Keys.NumSims;
import systemmanager.Keys.Periods;
import systemmanager.Keys.PricingPolicy;
import systemmanager.Keys.PrivateValueVar;
import systemmanager.Keys.RandomSeed;
import systemmanager.Keys.RangeA;
import systemmanager.Keys.RangeR;
import systemmanager.Keys.ReentryRate;
import systemmanager.Keys.ReentryType;
import systemmanager.Keys.Rmax;
import systemmanager.Keys.Rmin;
import systemmanager.Keys.SimLength;
import systemmanager.Keys.Size;
import systemmanager.Keys.Spread;
import systemmanager.Keys.Strats;
import systemmanager.Keys.Theta;
import systemmanager.Keys.ThetaMax;
import systemmanager.Keys.ThetaMin;
import systemmanager.Keys.TickImprovement;
import systemmanager.Keys.TickOutside;
import systemmanager.Keys.TickSize;
import systemmanager.Keys.Trunc;
import systemmanager.Keys.UseLastPrice;
import systemmanager.Keys.UseMedianSpread;
import systemmanager.Keys.W;
import systemmanager.Keys.Window;
import systemmanager.Keys.Withdraw;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;

import data.Stats;
import entity.agent.BackgroundAgent.Reentries;
import entity.market.Price;
import event.TimeStamp;
import fourheap.Order.OrderType;

/**
 * Stores ALL hard-coded defaults for simulation spec (environment) parameters 
 * and agent strategy parameters.
 *  
 * @author ewah
 *
 */
public class Defaults implements Serializable {

	private static final long serialVersionUID = -2159139939831086051L;
	
	public static <T> T get(Class<? extends Value<T>> key) {
		return defaults.get(key);
	}

	private final static ImmutableProps defaults = ImmutableProps.builder()
			// General
			.put(Num.class,				0)

			.put(TickSize.class,		1)
			
			.put(DiscountFactors.class,	Doubles.asList(0, 0.0006))
			.put(Periods.class,			Ints.asList(1, 250))
			
			.put(FeatureWhitelist.class, ImmutableList.of(Stats.SURPLUS + ".*_sum", Stats.PROFIT + ".*_sum",
					Stats.ZIRP_GREEDY + ".*_sum", Stats.MAX_EFF_POSITION + ".*_sum", Stats.NUM_TRANS + ".*_sum",
					Stats.FUNDAMENTAL_END_PRICE + ".*_mean", Stats.MARKET_MAKER_SPREAD + ".*_mean",
					Stats.CONTROL_FUNDAMENTAL + ".*_mean", Stats.CONTROL_PRIVATE_VALUE + ".*_mean", Stats.ZIRP_GREEDY + ".*_mean",
					Stats.MARKET_MAKER_SPREAD + ".*_stddev", Stats.PRICE + ".*_stddev"))
			
			// Simulation spec (general)
			.put(SimLength.class,		60000l)
			.put(FundamentalMean.class,	100000)
			.put(FundamentalKappa.class, 0.05)
			.put(FundamentalShockVar.class, 1000000d)
			.put(RandomSeed.class,		System.currentTimeMillis())
			.put(NumSims.class,			1)
			.put(NbboLatency.class,		TimeStamp.ZERO)
			.put(MarketLatency.class,	TimeStamp.ZERO)
			.put(FundamentalLatency.class, TimeStamp.ZERO)

			.put(PricingPolicy.class,	0.5)
			.put(ClearInterval.class,	TimeStamp.of(1000))
			
			// Agent-level defaults
			.put(ReentryType.class,		Reentries.EXPONENTIAL)
			.put(ReentryRate.class,		0.005)
			
			// Agent Types by Role
			// HFT Agents
			.put(LaLatency.class,		TimeStamp.ZERO)
			.put(Alpha.class,			0.001)
			
			// Background Agents
			.put(PrivateValueVar.class, 1000000d)
			.put(MaxPosition.class,		10)
			.put(Rmin.class,			0)
			.put(Rmax.class,			5000)
			.put(Window.class,	TimeStamp.of(5000))
			
			.put(AcceptableProfitThreshold.class, 1d) // For ZIRPs
			.put(Withdraw.class, true)
		
			// AA Agent
			.put(InitAggression.class,	0d)
			.put(Theta.class,			-4d)
			.put(ThetaMin.class,		-8d)
			.put(ThetaMax.class,		2d)
			.put(Eta.class,				3)
			.put(LambdaR.class,			0.05)
			.put(LambdaA.class,			0.02) // 0.02 in paper
			.put(Gamma.class,			2d)
			.put(BetaR.class,			0.4) // or U[0.2, 0.6]
			.put(BetaT.class,			0.4) // or U[0.2, 0.6]
			.put(BuyerStatus.class,		OrderType.BUY)
			.put(Debug.class,			false)
			
			// ZIP Agent
			.put(MarginMin.class,		0.05)
			.put(MarginMax.class,		0.35)
			.put(GammaMin.class,		0d)
			.put(GammaMax.class,		0.1)
			.put(BetaMin.class,			0.1)
			.put(BetaMax.class,			0.5)
			.put(RangeA.class,			0.05)
			.put(RangeR.class,			0.05)

			// Market Maker
			.put(K.class,		100)
			.put(Size.class,		1000)
			.put(Trunc.class,	true)
			.put(TickImprovement.class,	true)
			.put(TickOutside.class,		false)
			.put(InitLadderRange.class,	1000)
			// Keys.INITIAL_LADDER_MEAN should be set to fundamental mean in agent strategies that use it
			
			// MAMM
			.put(N.class,	5)
		
			// WMAMM
			.put(W.class,	0d)
		
			// AdaptiveMM
			.put(Strats.class,			Ints.asList(500, 1000, 2500, 5000))
			.put(UseMedianSpread.class,	false)
			.put(MovingAveragePrice.class, true)
			.put(FastLearning.class,	true)
			.put(UseLastPrice.class,	true)
			
			// Fundamental MM
			.put(FundEstimate.class,	Price.ZERO)
			.put(Spread.class,			Price.ZERO)
			
			// Build, put new defaults above this
			.build();
}
