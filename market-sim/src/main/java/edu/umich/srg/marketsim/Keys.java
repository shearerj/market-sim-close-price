package edu.umich.srg.marketsim;

import com.google.common.base.Converter;

import edu.umich.srg.egtaonline.spec.IgnoreValue;
import edu.umich.srg.egtaonline.spec.ParsableValue;
import edu.umich.srg.egtaonline.spec.ParsableValue.BoolValue;
import edu.umich.srg.egtaonline.spec.ParsableValue.DoubleValue;
import edu.umich.srg.egtaonline.spec.ParsableValue.EnumValue;
import edu.umich.srg.egtaonline.spec.ParsableValue.IntValue;
import edu.umich.srg.egtaonline.spec.ParsableValue.LongValue;
import edu.umich.srg.egtaonline.spec.ParsableValue.StringValue;
import edu.umich.srg.egtaonline.spec.ParsableValue.StringsValue;
import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.egtaonline.spec.ValueHelp;
import edu.umich.srg.fourheap.OrderType;
import edu.umich.srg.marketsim.agent.ZiAgent.OrderStyle;
import edu.umich.srg.marketsim.market.Benchmark.BenchmarkStyle;

/**
 * This file contains all of the type safe keys used in Spec object for initializing agents and
 * markets appropriately. These are initially read from strings in a simulation spec file, snd
 * therefore all of the keys must be in this file with public classes with public constructors so
 * that these classes can be properly instantiated. At the bottom of this file is a default spec
 * object. This is used to assign defaults for any parameters. In general this should be as sparse
 * as possible, but some keys have obvious defaults.
 */
public interface Keys {

  // ----------
  // Simulation
  // ----------

  @ValueHelp("The seed used to initialize all observations. If omitted, it is initialized with "
      + "the time.")
  class RandomSeed extends LongValue {
  }

  @ValueHelp("The number of time steps in the simulator.")
  class SimLength extends LongValue {
  }

  @ValueHelp("The markets that are constructed in the simulator.")
  class Markets extends StringsValue {
  }

  @ValueHelp("The mean of the gaussain fundamental.")
  class FundamentalMean extends DoubleValue {
  }

  @ValueHelp("The variance of the shocks in the fundamental.")
  class FundamentalShockVar extends DoubleValue {
  }

  @ValueHelp("The rate at which the fundamental reverts towards the mean.")
  class FundamentalMeanReversion extends DoubleValue {
  }

  @ValueHelp("The probability of a jump.")
  class FundamentalShockProb extends DoubleValue {
  }

  // -------
  // Markets
  // -------

  @ValueHelp("The type of benchmark used.")
  class BenchmarkType extends EnumValue<BenchmarkStyle> {
	    public BenchmarkType() {
	        super(BenchmarkStyle.class);
	      }
	    }
  
  @ValueHelp("Whether the call market biases towards buyer or seller, 0 -> buyer price, 1 -> "
      + "seller price.")
  class Pricing extends DoubleValue {
  }

  @ValueHelp("The clearing interval for a call market.")
  class ClearInterval extends LongValue {
  }

  // ------
  // Agents
  // ------

  @ValueHelp("The probability that an agent arrives at any time step.")
  class ArrivalRate extends DoubleValue {
  }

  @ValueHelp("Variance of added noise to any fundamental observation.")
  class FundamentalObservationVariance extends DoubleValue {
  }

  @ValueHelp("Whether an agent should buy or sell.")
  class Type extends EnumValue<OrderType> {
    public Type() {
      super(OrderType.class);
    }
  }

  @ValueHelp("The maximum absolute position an agent can hold.")
  class MaxPosition extends IntValue {
  }

  @ValueHelp("Variance used in generating an agents private value.")
  class PrivateValueVar extends DoubleValue {
  }

  @ValueHelp("The maximum surplus an agent will demand.")
  class Rmax extends IntValue {
  }

  @ValueHelp("The minimum surplus an agent will demand.")
  class Rmin extends IntValue {
  }

  @ValueHelp("How agents should decide what side to pick.")
  class Sides extends EnumValue<OrderStyle> {
    public Sides() {
      super(OrderStyle.class);
    }
  }

  @ValueHelp("How many orders per side an agent should submit.")
  class SubmitDepth extends IntValue {
  }

  // -------------------------
  // Specific agent parameters
  // -------------------------
  
  @ValueHelp("Markov agent's estimate variance for price relative to the fundamental.")
  class PriceVarEst extends DoubleValue {
  }
  
  @ValueHelp("Markov agent's estimate variance for price relative to the fundamental.")
  class ShareEstimates extends BoolValue {
  }

  @ValueHelp("Number of order the shock agent will submit.")
  class NumShockOrders extends IntValue {
  }

  @ValueHelp("The fraction of demanded surplus necessary for an agent to submit an order at bid "
      + "or ask.")
  class Thresh extends DoubleValue {
  }

  @ValueHelp("Amount of time the shock agent has to liquidate.")
  class TimeToLiquidate extends LongValue {
  }
  
  //----------------
  // Benchmark Agent
  //----------------
  @ValueHelp("The price impact on an agent's quote submission, must be a positive value.")
  class BenchmarkImpact extends IntValue {
  }
  
  @ValueHelp("The amount of external contract holdings an agent possesses, must be in [0, 1].")
  class ContractHoldings extends DoubleValue {
  }
  
  @ValueHelp("The direction of external contract holdings an agent uses to adjust desired surplus, must be in {-1,0,1}.")
  class BenchmarkDir extends IntValue {
  }
  
  @ValueHelp("The fraction of demanded surplus necessary for a benchmark agent to submit an order at bid or ask.")
  class BenchmarkThresh extends DoubleValue {
  }
  
  @ValueHelp("The fixed book depth level for training in deep RL.")
  class ViewBookDepth extends IntValue {
  }
  
  @ValueHelp("The fixed length of obsevable transactions for training in deep RL.")
  class TransactionDepth extends IntValue {
  }
  
  @ValueHelp("The fixed length of obsevable transactions for omega ratio when training in deep RL.")
  class OmegaDepth extends IntValue {
  }
  
  @ValueHelp("The maximum fixed length of any vector (e.g. bid and ask vectors, and transaction history) passed through the TensorFlow graph when using RL.")
  class MaxVectorDepth extends IntValue {
  }
  
  @ValueHelp("Boolean value which determines whether the deep RL benchmark agent chooses its action by a policy or randomly.")
  class PolicyAction extends BoolValue {
  }
  
  @ValueHelp("Coefficient used when mapping actions from [1,1] to prices, used for Deep RL agent.")
  class ActionCoefficient extends DoubleValue {
  }
  
  @ValueHelp("The Python path for deep RL model when using policy-based action.")
  class PythonModelPath extends StringValue {
  }
  
  @ValueHelp("The Tensorflow path for Tensor graph to interact with RL Python agent.")
  class TensorFlowModelPath extends StringValue {
  }
  
  @ValueHelp("The path for deep RL paramters when using policy-based action.")
  class BenchmarkParamPath extends StringValue {
  }

  @ValueHelp("Job number for greatlakes, used only for file labels in python action.")
  class GreatLakesJobNumber extends IntValue {
  }
  
  @ValueHelp("Size of state space.")
  class NbStates extends IntValue {
  }
  
  
  @ValueHelp("Size of action space.")
  class NbActions extends IntValue {
  }
  
  @ValueHelp("Size of first hidden layer")
  class HiddenLayer1 extends IntValue {
  }
  
  @ValueHelp("Size of second hidden layer")
  class HiddenLayer2 extends IntValue {
  }
  
  @ValueHelp("Weight matrices for DRL model.")
  class ActorWeights extends StringsValue {
  }
  
  @ValueHelp("Flags for state space to determine which features are included.")
  class StateSpaceFlags extends StringsValue {
  }
  
  @ValueHelp("Is a Deep RL agent training or not.")
  class IsTraining extends BoolValue {
  }
  
  @ValueHelp("Mu parameter for OU noise.")
  class OUMu extends DoubleValue {
  }
  
  @ValueHelp("Sigma parameter for OU noise.")
  class OUSigma extends DoubleValue {
  }
  
  @ValueHelp("Theta parameter for OU noise.")
  class OUTheta extends DoubleValue {
  }
  
  @ValueHelp("Parameter for noise decay.")
  class EpsilonDecay extends DoubleValue {
  }
  
  @ValueHelp("Latency, or time delay, in time steps for an RL agent. If latency is 5, then the market waits 5 time steps to process request or notify of transaction.")
  class CommunicationLatency extends LongValue {
  }
  
  @ValueHelp("Actions to include in the Tensorflow graph output that are not price, side, and size. Should be a string with each additional action seperated by \'/\', e.g. \"rmin/rmax/thresh\" ")
  class AdditionalActions extends StringsValue {
  }

  // ------------
  // Market Maker
  // ------------
  @ValueHelp("Separation between successive rungs.")
  class RungSep extends IntValue {
  }

  @ValueHelp("Number of rungs to submit.")
  class NumRungs extends IntValue {
  }

  @ValueHelp("Whether to tweak rung by a tick.")
  class TickImprovement extends BoolValue {
  }

  @ValueHelp("Place tick improvement outside spread, instead of default inside.")
  class TickOutside extends BoolValue {
  }

  @ValueHelp("Number of orders per rung.")
  class RungThickness extends IntValue {
  }

  @ValueHelp("Spread between best bid and best ask.")
  class Spread extends DoubleValue {
  }

  // -------------------
  // Trend Following HFT
  // -------------------

  @ValueHelp("The minimum length of a monotonic trend to act.")
  class TrendLength extends IntValue {
  }

  @ValueHelp("The maximum profit demanded from a trend front run.")
  class ProfitDemanded extends IntValue {
  }

  @ValueHelp("The max amount of time to leave an order in the market.")
  class Expiration extends TimeValue {
  }

  // -------------------
  // Call Market Agent
  // -------------------

  @ValueHelp("Fraction of [rmin, rmax] to use at the beginning of a call interval.")
  class InitialFrac extends DoubleValue {
  }

  @ValueHelp("Fraction of [rmin, rmax] to use at the end of a call interval.")
  class FinalFrac extends DoubleValue {
  }

  /* Old leftover keys. */

  // public static class DiscountFactors extends DoublesValue {};
  //
  // public static class FileName extends StringValue {};
  //
  // public static class Window extends TimeValue {};
  // public static class Alpha extends DoubleValue {};
  //
  //
  //
  // public static class Num extends IntValue {};
  // public static class NumAgents extends IntValue {};
  // public static class NumMarkets extends IntValue {};

  // Latency
  // public static class NbboLatency extends TimeValue {};
  // public static class MarketLatency extends TimeValue {};
  // public static class LaLatency extends TimeValue {};
  // public static class FundamentalLatency extends TimeValue {};

  // Call Market
  // public static class ClearInterval extends TimeValue {};
  // public static class PricingPolicy extends DoubleValue {};

  // Agents
  // public static class Withdraw extends BoolValue {};
  // public static class N extends IntValue {};

  // public static class ReentryType extends EnumValue<Reentries> { public ReentryType() {
  // super(Reentries.class); }};

  // Market Maker
  // public static class K extends IntValue {};
  // public static class Size extends IntValue {};
  // public static class Trunc extends BoolValue {};
  // public static class InitLadderMean extends IntValue {};
  // public static class InitLadderRange extends IntValue {};

  // AAAgent
  // public static class Eta extends IntValue {};
  // public static class LambdaR extends DoubleValue {};
  // public static class LambdaA extends DoubleValue {};
  // public static class Gamma extends DoubleValue {};
  // public static class BetaR extends DoubleValue {};
  // public static class BetaT extends DoubleValue {};
  // public static class InitAggression extends DoubleValue {};
  // public static class Theta extends DoubleValue {};
  // public static class ThetaMax extends DoubleValue {};
  // public static class ThetaMin extends DoubleValue {};
  // public static class Debug extends BoolValue {};
  // public static class BuyerStatus extends EnumValue<OrderType> { public BuyerStatus() {
  // super(OrderType.class); }};

  // ZIPAgent
  // public static class MarginMin extends DoubleValue {};
  // public static class MarginMax extends DoubleValue {};
  // public static class GammaMin extends DoubleValue {};
  // public static class GammaMax extends DoubleValue {};
  // public static class BetaMin extends DoubleValue {};
  // public static class BetaMax extends DoubleValue {};
  // public static class RangeR extends DoubleValue {};
  // public static class RangeA extends DoubleValue {};

  // Market Makers
  // public static class W extends DoubleValue {};
  // public static class Strats extends IntsValue {};
  // public static class UseMedianSpread extends BoolValue {};
  // public static class MovingAveragePrice extends BoolValue {};
  // public static class FastLearning extends BoolValue {};
  // public static class UseLastPrice extends BoolValue {};
  // public static class FundEstimate extends PriceValue {};
  // public static class Spread extends PriceValue {};

  // ZIRPAgent


  // --------------
  // Helper Classes
  // --------------

  /** Spec parameter that's a TimeStamp. */
  @IgnoreValue
  class TimeValue extends ParsableValue<TimeStamp> {
    protected TimeValue() {
      super(new Converter<String, TimeStamp>() {
        @Override
        protected String doBackward(TimeStamp time) {
          return Long.toString(time.get());
        }

        @Override
        protected TimeStamp doForward(String string) {
          return TimeStamp.of(Long.parseLong(string));
        }
      });
    }
  }

  // static class PriceValue extends ParsableValue<Price> {
  // protected PriceValue() {
  // super(new Converter<String, Price>() {
  // @Override protected String doBackward(Price price) { return Long.toString(price.longValue()); }
  // @Override protected Price doForward(String string) { return Price.of(Long.parseLong(string)); }
  // });
  // }
  // }

  /**
   * The simulation will use these as the defaults for any unspecified keys. This should be as
   * sparse as possible, and only used when one shouldn't be forced to manually specify something.
   */
  Spec DEFAULT_KEYS = Spec.builder() //
      .put(RandomSeed.class, System.nanoTime()) // Set seed from clock
      .put(FundamentalMean.class, 1e9) // Approximately half of Integer.MAX_VALUE
      .put(FundamentalObservationVariance.class, 0d) // Perfect revelation

      .put(Pricing.class, 0.5) // Even call market

      .put(Sides.class, OrderStyle.RANDOM) // Submit orders randomly (legacy)
      .put(SubmitDepth.class, 1) // Submit one order per arrival (legacy)
      .put(Thresh.class, 1d) // No threshold
      .put(BenchmarkThresh.class, 1d) // No threshold
      .put(ContractHoldings.class, 0.0)
      .put(BenchmarkDir.class, 1)
      .put(ShareEstimates.class, false) // Don't share estimates unless explicit
      .put(PolicyAction.class, false) // Randomly generate actionCoefficient
      .put(GreatLakesJobNumber.class, -1) // Randomly generate action
      .put(NbStates.class, 0) // Assign arbitrary number of states
      .put(NbActions.class, 0) // Assign arbitrary number of actions
      .put(HiddenLayer1.class, 0) // Assign arbitrary size of first hidden layer
      .put(HiddenLayer2.class, 0) // Assign arbitrary size of second hidden layer
      .put(IsTraining.class, true) // Assumes that model is training when generating actions
      .put(OUMu.class, 0.0) // Assign arbitrary number for OU parameter mu
      .put(OUSigma.class, 0.0) // Assign arbitrary number for OU parameter sigma
      .put(OUTheta.class, 0.0) // Assign arbitrary number for OU parameter theta
      .put(EpsilonDecay.class, 0.0) // Assign arbitrary number for epsilon decay for DRL
      .put(CommunicationLatency.class, 0L) // Assign zero latency for RL agent
      .put(BenchmarkType.class, BenchmarkStyle.VWAP) //Use volume-weighted price average (VWAP) as a market benchmark
      .build();

}

