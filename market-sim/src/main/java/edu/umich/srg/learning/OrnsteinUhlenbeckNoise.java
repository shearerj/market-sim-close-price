package edu.umich.srg.learning;

import java.util.Random;

import edu.umich.srg.distributions.Gaussian;
import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.marketsim.Sim;
import no.uib.cipr.matrix.DenseMatrix;

import edu.umich.srg.marketsim.Keys.EpsilonDecay;
import edu.umich.srg.marketsim.Keys.OUMu;
import edu.umich.srg.marketsim.Keys.OUSigma;
import edu.umich.srg.marketsim.Keys.OUTheta;
import edu.umich.srg.marketsim.Keys.SimLength;

public class OrnsteinUhlenbeckNoise{
	
	private Sim sim;
	private Gaussian nNoise;
	private Random rand;
	private final double mu;
	private final double sigma;
	private final double theta;
	private long prevTimeStep;
	private double prevNoise;
	private long simLength;
	
	public OrnsteinUhlenbeckNoise(Sim sim, Spec spec, Random rand) {
	    
	    this.sim = sim;
	    this.rand = rand;
		this.nNoise = Gaussian.withMeanStandardDeviation(0,1);
	    this.mu = spec.get(OUMu.class);
	    this.sigma = spec.get(OUSigma.class);
	    this.theta = spec.get(OUTheta.class);
	    this.prevTimeStep = 0;
	    this.prevNoise = 0;
	    this.simLength = spec.get(SimLength.class);
			
	}
		
	public static OrnsteinUhlenbeckNoise create(Sim sim, Spec spec, Random rand) {
		return new OrnsteinUhlenbeckNoise(sim, spec, rand);
	}
	
	public double ouNoise() {
		long currTimeStep = sim.getCurrentTime().get();
		double deltaT = ((double)(currTimeStep - this.prevTimeStep)) / ((double) this.simLength);
		double noise = this.prevNoise;
		noise += this.theta * deltaT * (this.mu - this.prevNoise);
		noise += sigma * Math.sqrt(deltaT) * nNoise.sample(rand);
		
		this.prevNoise = noise;
		this.prevTimeStep = currTimeStep;
		return noise;
	}
	
	public DenseMatrix ouNoiseMtx(int c) {
		DenseMatrix noise = new DenseMatrix(1,c);
		for(int j=0; j<c; j++) {
			noise.add(0, j, this.ouNoise());
		}
		return noise;
	}
	
}