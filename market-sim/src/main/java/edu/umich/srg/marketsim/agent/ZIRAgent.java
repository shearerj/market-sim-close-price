package edu.umich.srg.marketsim.agent;

import static com.google.common.base.Preconditions.checkArgument;
import static edu.umich.srg.fourheap.Order.OrderType.BUY;
import static java.math.RoundingMode.CEILING;
import static java.math.RoundingMode.FLOOR;

import java.util.Collection;
import java.util.Random;

import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;
import com.google.gson.JsonObject;

import edu.umich.srg.distributions.Distribution;
import edu.umich.srg.distributions.Geometric;
import edu.umich.srg.distributions.Uniform;
import edu.umich.srg.distributions.Uniform.IntUniform;
import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.fourheap.Order.OrderType;
import edu.umich.srg.marketsim.Keys.ArrivalRate;
import edu.umich.srg.marketsim.Keys.FundamentalMean;
import edu.umich.srg.marketsim.Keys.FundamentalMeanReversion;
import edu.umich.srg.marketsim.Keys.MaxPosition;
import edu.umich.srg.marketsim.Keys.PrivateValueVar;
import edu.umich.srg.marketsim.Keys.Rmax;
import edu.umich.srg.marketsim.Keys.Rmin;
import edu.umich.srg.marketsim.Keys.SimLength;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.fundamental.FundamentalView;
import edu.umich.srg.marketsim.fundamental.GaussianMeanReverting;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.market.OrderRecord;
import edu.umich.srg.marketsim.privatevalue.GaussianPrivateValue;
import edu.umich.srg.marketsim.privatevalue.PrivateValue;
import edu.umich.srg.marketsim.strategy.GaussianFundamentalEstimator;

public class ZIRAgent implements Agent {
	
	private static final Distribution<OrderType> orderTypeDistribution = Uniform.over(OrderType.values());
	
	private final Sim sim;
	private final MarketView market;
	private final FundamentalView fundamental;
	private final GaussianFundamentalEstimator estimator;
	private final int maxPosition;
	private final PrivateValue privateValue;
	private final Random rand;
	private final Geometric arrivalDistribution;
	private final IntUniform shadingDistribution;
	
	public ZIRAgent(Sim sim, Market market, Fundamental fundamental, Spec spec, Random rand) {
		checkArgument(fundamental instanceof GaussianMeanReverting);
		
		this.sim = sim;
		this.market = market.getView(this, TimeStamp.ZERO);
		this.fundamental = FundamentalView.create(sim, fundamental);
		this.estimator = new GaussianFundamentalEstimator(spec.get(SimLength.class), spec.get(FundamentalMean.class), spec.get(FundamentalMeanReversion.class));
		this.maxPosition = spec.get(MaxPosition.class);
		this.privateValue = GaussianPrivateValue.generate(rand, spec.get(MaxPosition.class), spec.get(PrivateValueVar.class));
		this.arrivalDistribution = Geometric.withSuccessProbability(spec.get(ArrivalRate.class));
		this.shadingDistribution = Uniform.discreteClosed(spec.get(Rmin.class), spec.get(Rmax.class));
		this.rand = rand;
	}
	
	public static ZIRAgent createFromSpec(Sim sim, Fundamental fundamental, Collection<Market> markets, Market market, Spec spec, Random rand) {
		return new ZIRAgent(sim, market, fundamental, spec, rand);
	}
	
	private void scheduleNextArrival() {
		sim.scheduleIn(TimeStamp.of(1 + arrivalDistribution.sample(rand)), this::strategy);
	}
	
	private void strategy() {
		for (OrderRecord order : ImmutableList.copyOf(market.getActiveOrders()))
			market.withdrawOrder(order);

		OrderType type = orderTypeDistribution.sample(rand);
		double finalEstimate = estimator.estimate(sim.getCurrentTime(), fundamental.getFundamental()),
				// FIXME verify this is the relation I want
				privateBenefit = type.sign() * privateValue.valueForExchange(market.getHoldings(), type),
				shade = type.sign() * shadingDistribution.sample(rand);
		// Round beneficially
		long roundedPrice =  DoubleMath.roundToLong(finalEstimate + privateBenefit - shade, type == BUY ? FLOOR : CEILING);
		if (roundedPrice > 0 && Math.abs(market.getHoldings() + type.sign()) <= maxPosition)
			market.submitOrder(type, Price.of(roundedPrice), 1);
		scheduleNextArrival();
	}
	
	@Override
	public void initilaize() {
		scheduleNextArrival();
	}
	
	@Override
	public double payoffForPosition(int position) {
		return privateValue.valueAtPosition(position);
	}
	
	@Override
	public JsonObject getFeatures() {
		return new JsonObject();
	}
	
	@Override
	public String toString() {
		return "ZIR " + Integer.toString(System.identityHashCode(this), 36).toUpperCase();
	}

	// Notifications
	
	@Override
	public void notifyOrderSubmitted(OrderRecord order) { }

	@Override
	public void notifyOrderWithrawn(OrderRecord order, int quantity) { }

	@Override
	public void notifyOrderTransacted(OrderRecord order, Price price, int quantity) { }

	@Override
	public void notifyQuoteUpdated(MarketView market) { }

}
