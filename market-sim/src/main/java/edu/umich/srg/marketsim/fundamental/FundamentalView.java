package edu.umich.srg.marketsim.fundamental;

import java.io.Serializable;
import java.util.Random;

import edu.umich.srg.distributions.Gaussian;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.TimeStamp;

public interface FundamentalView {

	Price getFundamental();
	
	static FundamentalView create(Sim sim, Fundamental fundamental) {
		return new CurrentFundamental(sim, fundamental);
	}
	
	static FundamentalView create(Sim sim, Fundamental fundamental, TimeStamp latency) {
		if (latency.equals(TimeStamp.ZERO))
			return new CurrentFundamental(sim, fundamental);
		else
			return new DelayedFundamental(sim, fundamental, latency);
	}
	
	static FundamentalView create(Sim sim, Fundamental fundamental, TimeStamp latency, double noiseVariance, Random rand) {
		if (latency.equals(TimeStamp.ZERO) && noiseVariance == 0)
			return new CurrentFundamental(sim, fundamental);
		else if (noiseVariance == 0)
			return new DelayedFundamental(sim, fundamental, latency);
		else
			return new NoisyDelayedFundamental(sim, fundamental, latency, noiseVariance, rand);
	}
	
	static class NoisyDelayedFundamental implements FundamentalView, Serializable {

		private final Sim sim;
		private final Fundamental fundamental;
		private long latency;
		private Gaussian noise;
		private Random rand;
		
		private NoisyDelayedFundamental(Sim sim, Fundamental fundamental, TimeStamp latency, double variance, Random rand) {
			this.sim = sim;
			this.fundamental = fundamental;
			this.latency = latency.get();
			this.noise = Gaussian.withMeanVariance(0, variance);
			this.rand = rand;
		}

		@Override
		public Price getFundamental() {
			Price fundamentalValue = fundamental.getValueAt(TimeStamp.of(sim.getCurrentTime().get() - latency));
			return Price.of(fundamentalValue.doubleValue() + noise.sample(rand));
		}
		
		private static final long serialVersionUID = -8417692791904477957L;
		
	}
	
	static class DelayedFundamental implements FundamentalView, Serializable {

		private final Sim sim;
		private final Fundamental fundamental;
		private long latency;
		
		private DelayedFundamental(Sim sim, Fundamental fundamental, TimeStamp latency) {
			this.sim = sim;
			this.fundamental = fundamental;
			this.latency = latency.get();
		}
		
		@Override
		public Price getFundamental() {
			return fundamental.getValueAt(TimeStamp.of(sim.getCurrentTime().get() - latency));
		}
		
		private static final long serialVersionUID = -3705762902689938109L;
		
	}
	
	static class CurrentFundamental implements FundamentalView, Serializable {

		private final Sim sim;
		private final Fundamental fundamental;
		
		private CurrentFundamental(Sim sim, Fundamental fundamental) {
			this.sim = sim;
			this.fundamental = fundamental;
		}
		
		@Override
		public Price getFundamental() {
			return fundamental.getValueAt(sim.getCurrentTime());
		}
		
		private static final long serialVersionUID = 8104569321458703510L;
		
	}
	
}
