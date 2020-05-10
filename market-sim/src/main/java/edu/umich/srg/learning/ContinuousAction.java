package edu.umich.srg.learning;

import java.util.Random;

import com.google.gson.JsonArray;

import edu.umich.srg.distributions.Uniform;
import edu.umich.srg.distributions.Uniform.ContinuousUniform;
import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.marketsim.Keys.ActionCoefficient;


public class ContinuousAction implements Action {
	
	private ContinuousUniform actionsToSubmit;
	private final double actionCoefficient;
	private Random rand;
	private double alpha;
	private int actionSize;
	
	protected ContinuousAction(Spec spec, Random rand) {
			
		this.rand = rand;
		this.actionsToSubmit = Uniform.closedOpen(-1.0, 1.0);
		this.actionCoefficient = spec.get(ActionCoefficient.class);
		this.alpha = 0;
		this.actionSize = 0;
			
	}
		
	public static ContinuousAction create(Spec spec, Random rand) {
		return new ContinuousAction(spec, rand);
	}

	@Override
	public JsonArray getAction() {
		JsonArray action = new JsonArray();
		
		//ContinuousUniform actionsToSubmit = Uniform.closedOpen(-1.0, 1.0);
        this.alpha = actionsToSubmit.sample(this.rand);
        action.add(this.alpha);
        this.actionSize = action.size();
		return action;
	}

	@Override
	public int getActionSize() {
		return this.actionSize;
	}

	@Override
	public double actionToPrice(double finalEstimate) {
		int alpha_sign = 1;
	  	if (this.alpha < 0) {alpha_sign = -1;};
	    double toSubmit = finalEstimate + alpha_sign * this.actionCoefficient * Math.exp(this.alpha);
		return toSubmit;
	}

	@Override
	public JsonArray getAction(String curr_state) {
		// TODO Auto-generated method stub
		return null;
	}
	
}