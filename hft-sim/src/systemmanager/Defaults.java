package systemmanager;

import java.io.Serializable;
import java.util.Arrays;

import props.ImmutableProps;
import props.Value;
import systemmanager.Keys.AcceptableProfitFrac;
import systemmanager.Keys.AgentTickSize;
import systemmanager.Keys.Alpha;
import systemmanager.Keys.ArrivalRate;
import systemmanager.Keys.BetaMax;
import systemmanager.Keys.BetaMin;
import systemmanager.Keys.BetaR;
import systemmanager.Keys.BetaT;
import systemmanager.Keys.BidRangeMax;
import systemmanager.Keys.BidRangeMin;
import systemmanager.Keys.BuyerStatus;
import systemmanager.Keys.ClearFrequency;
import systemmanager.Keys.Debug;
import systemmanager.Keys.DiscountFactors;
import systemmanager.Keys.Eta;
import systemmanager.Keys.FastLearning;
import systemmanager.Keys.FundamentalKappa;
import systemmanager.Keys.FundamentalLatency;
import systemmanager.Keys.FundamentalMean;
import systemmanager.Keys.FundamentalShockVar;
import systemmanager.Keys.Gamma;
import systemmanager.Keys.GammaMax;
import systemmanager.Keys.GammaMin;
import systemmanager.Keys.InitAggression;
import systemmanager.Keys.InitLadderRange;
import systemmanager.Keys.LaLatency;
import systemmanager.Keys.LambdaA;
import systemmanager.Keys.LambdaR;
import systemmanager.Keys.MarginMax;
import systemmanager.Keys.MarginMin;
import systemmanager.Keys.MarketLatency;
import systemmanager.Keys.MarketTickSize;
import systemmanager.Keys.MaxQty;
import systemmanager.Keys.MeanPrefixes;
import systemmanager.Keys.MovingAveragePrice;
import systemmanager.Keys.NbboLatency;
import systemmanager.Keys.Num;
import systemmanager.Keys.NumAgents;
import systemmanager.Keys.NumHistorical;
import systemmanager.Keys.NumMarkets;
import systemmanager.Keys.NumRungs;
import systemmanager.Keys.NumSims;
import systemmanager.Keys.Periods;
import systemmanager.Keys.PricingPolicy;
import systemmanager.Keys.PrivateValueVar;
import systemmanager.Keys.RandomSeed;
import systemmanager.Keys.RangeA;
import systemmanager.Keys.RangeR;
import systemmanager.Keys.ReentryRate;
import systemmanager.Keys.RungSize;
import systemmanager.Keys.SimLength;
import systemmanager.Keys.Spreads;
import systemmanager.Keys.StddevPrefixes;
import systemmanager.Keys.SumPrefixes;
import systemmanager.Keys.Theta;
import systemmanager.Keys.ThetaMax;
import systemmanager.Keys.ThetaMin;
import systemmanager.Keys.TickImprovement;
import systemmanager.Keys.TickOutside;
import systemmanager.Keys.TickSize;
import systemmanager.Keys.TruncateLadder;
import systemmanager.Keys.UseLastPrice;
import systemmanager.Keys.UseMedianSpread;
import systemmanager.Keys.WeightFactor;
import systemmanager.Keys.WindowLength;
import systemmanager.Keys.WithdrawOrders;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;

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
	
	// TODO reduce size of strategy strings (they're all way too long, provide some better guidelines on usage
	
	public static <T> T get(Class<? extends Value<T>> key) {
		return defaults.get(key);
	}

	private final static ImmutableProps defaults = ImmutableProps.builder()
			// General
			.put(Num.class,				0)
			.put(NumAgents.class,		0)
			.put(NumMarkets.class,		1)

			.put(TickSize.class,		1)
			.put(MarketTickSize.class,	1)
			.put(AgentTickSize.class,	1)
			
			.put(DiscountFactors.class,	Doubles.asList(0, 0.0006))
			.put(Periods.class,			Ints.asList(1, 250))
			
			.put(SumPrefixes.class,		Arrays.<String> asList())
			.put(MeanPrefixes.class,	Arrays.<String> asList())
			.put(StddevPrefixes.class,	Arrays.<String> asList())
			
			// Simulation spec (general)
			.put(SimLength.class,		60000)
			.put(FundamentalMean.class,	100000)
			.put(FundamentalKappa.class, 0.05)
			.put(FundamentalShockVar.class, 1000000d)
			.put(RandomSeed.class,		System.currentTimeMillis())
			.put(NumSims.class,			1)
			.put(NbboLatency.class,		TimeStamp.ZERO)
			.put(MarketLatency.class,	TimeStamp.ZERO)
			.put(FundamentalLatency.class, TimeStamp.ZERO)

			.put(PricingPolicy.class,	0.5)
			.put(ClearFrequency.class,	TimeStamp.of(1000))
		
			// Agent-level defaults
			.put(ArrivalRate.class,		0.075)
			.put(ReentryRate.class,		0.005)
			
			// Agent Types by Role
			// HFT Agents
			.put(LaLatency.class,		TimeStamp.ZERO)
			.put(Alpha.class,			0.001)

			// Background Agents
			.put(PrivateValueVar.class, 1000000d)
			.put(MaxQty.class,			10)
			.put(BidRangeMin.class,		0)
			.put(BidRangeMax.class,		5000)
			.put(WindowLength.class,	TimeStamp.of(5000))
			
			.put(AcceptableProfitFrac.class, 0.8) // For ZIRPs
			.put(WithdrawOrders.class, true) // for ZIRs
		
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
			.put(NumRungs.class,		100)
			.put(RungSize.class,		1000)
			.put(TruncateLadder.class,	true)
			.put(TickImprovement.class,	true)
			.put(TickOutside.class,		false)
			.put(InitLadderRange.class,	1000)
			// Keys.INITIAL_LADDER_MEAN should be set to fundamental mean in agent strategies that use it
		
			// MAMM
			.put(NumHistorical.class,	5)
		
			// WMAMM
			.put(WeightFactor.class,	0d)
		
			// AdaptiveMM
			.put(Spreads.class,			Ints.asList(500, 1000, 2500, 5000))
			.put(UseMedianSpread.class,	false)
			.put(MovingAveragePrice.class, true)
			.put(FastLearning.class,	true)
			.put(UseLastPrice.class,	true)
			
			// Build, put new defaults above this
			.build();
	
}
