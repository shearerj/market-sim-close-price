package edu.umich.srg.learning;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import edu.umich.srg.distributions.Uniform;
import edu.umich.srg.distributions.Uniform.ContinuousUniform;
import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.fourheap.OrderType;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.Keys.ActionCoefficient;

import edu.umich.srg.marketsim.Keys.TensorFlowModelPath;
// import edu.umich.srg.marketsim.Keys.GreatLakesJobNumber;
// import edu.umich.srg.marketsim.Keys.NbActions;
// import edu.umich.srg.marketsim.Keys.NbStates;
// import edu.umich.srg.marketsim.Keys.IsTraining;

import org.tensorflow.Graph;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.DataType;
import org.tensorflow.TensorFlow;
import org.tensorflow.types.TFloat32;
import org.tensorflow.types.TFloat64;
import org.tensorflow.types.TInt32;
import org.tensorflow.types.TString;
import org.tensorflow.types.family.TType;

public class TensorFlowAction extends ContinuousAction {

	// private ContinuousUniform actionsToSubmit;
	private Random rand;
	private double alpha;
	
	private final String TFModelPath;
	// private final int glJobNum;
	// private final int nbStates;
	// private final int nbActions;
	// private final Boolean isTraining;

	private int actionSize;

	public TensorFlowAction(Sim sim, Spec spec, Random rand) {
		super(spec, rand);
		// this.actionsToSubmit = Uniform.closedOpen(-1.0, 1.0);
		this.alpha = 0;
		this.TFModelPath = spec.get(TensorFlowModelPath.class).iterator().next();
		this.actionSize = 0;
	    // this.glJobNum = spec.get(GreatLakesJobNumber.class);
	    // this.nbStates = spec.get(NbStates.class);
	    // this.nbActions = spec.get(NbActions.class);
	    // this.isTraining = spec.get(IsTraining.class);
	}

	public static TensorFlowAction create(Sim sim, Spec spec, Random rand) {
		return new TensorFlowAction(sim, spec, rand);
	}

	@Override
	public JsonObject getActionDict(JsonObject state, double finalEstimate) {
		JsonObject action = new JsonObject();
		try(SavedModelBundle savedModelBundle = SavedModelBundle.load(this.TFModelPath, "serve")) {
			Session session = savedModelBundle.session();
		
			//@SuppressWarnings("unchecked")
			List<Tensor<?>> order = (List<Tensor<?>>) session.runner()
					.feed("finalFundamentalEstimate", TFloat64.scalarOf(state.get("finalFundamentalEstimate").getAsDouble()))
					.feed("side", TInt32.scalarOf(state.get("side").getAsInt()))
					.feed("bidSize", TInt32.scalarOf(state.get("bidSize").getAsInt()))
					.feed("askSize", TInt32.scalarOf(state.get("askSize").getAsInt()))
					.feed("spread", TInt32.scalarOf(state.get("spread").getAsInt()))
					.feed("marketHoldings", TInt32.scalarOf(state.get("marketHoldings").getAsInt()))
					.feed("timeTilEnd", TInt32.scalarOf(state.get("timeTilEnd").getAsInt()))
					.feed("bidVector", TString.vectorOf(state.get("bidVector").getAsString()))
					.feed("askVector", TString.vectorOf(state.get("askVector").getAsString()))
					.feed("transactionHistory", TString.vectorOf(state.get("transactionHistory").getAsString()))
					.fetch("price", 0)
					.fetch("side", 1)
					.fetch("size", 2)
					.run();
			
			action.addProperty("price", Double.parseDouble(String.valueOf(order.get(0))));
			action.addProperty("side", Integer.parseInt(String.valueOf(order.get(1))));
			action.addProperty("size", Integer.parseInt(String.valueOf(order.get(2))));

			return action;

		}
	}
}
