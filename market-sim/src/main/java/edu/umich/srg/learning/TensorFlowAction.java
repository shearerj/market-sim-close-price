package edu.umich.srg.learning;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.marketsim.Sim;

import edu.umich.srg.marketsim.Keys.TensorFlowModelPath;
// import edu.umich.srg.marketsim.Keys.GreatLakesJobNumber;
// import edu.umich.srg.marketsim.Keys.NbActions;
// import edu.umich.srg.marketsim.Keys.NbStates;
// import edu.umich.srg.marketsim.Keys.IsTraining;

import org.tensorflow.Graph;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.Tensors;
import org.tensorflow.DataType;
import org.tensorflow.TensorFlow;
import org.tensorflow.types.TFloat64;
import org.tensorflow.types.TInt32;
import org.tensorflow.types.TString;
import org.tensorflow.types.family.TType;
import org.tensorflow.ndarray.DoubleNdArray;
import org.tensorflow.op.core.Shape;

public class TensorFlowAction extends ContinuousAction {

	// private ContinuousUniform actionsToSubmit;
	
	private final String TFModelPath;
	// private final int glJobNum;
	// private final int nbStates;
	// private final int nbActions;
	// private final Boolean isTraining;


	public TensorFlowAction(Sim sim, Spec spec, Random rand) {
		super(spec, rand);
		this.TFModelPath = spec.get(TensorFlowModelPath.class).iterator().next();
	}

	public static TensorFlowAction create(Sim sim, Spec spec, Random rand) {
		return new TensorFlowAction(sim, spec, rand);
	}

	@Override
	public JsonObject getActionDict(JsonObject state, double finalEstimate) {
		JsonObject action = new JsonObject();
		try(SavedModelBundle savedModelBundle = SavedModelBundle.load(this.TFModelPath, "serve")) {
			Session session = savedModelBundle.session();
			//Tensor bidVector = 
			//Tensor bidVec = Tensor.create(new int[] {1});
			Tensor<?> bidVector = this.jsonToTensor(state.get("bidVector").getAsJsonArray(), 20);
			Tensor<?> askVector = this.jsonToTensor(state.get("askVector").getAsJsonArray(), 20);
			Tensor<?> transactionHistory = this.jsonToTensor(state.get("transactionHistory").getAsJsonArray(), 20);
	
			List<Tensor<?>> order = (List<Tensor<?>>) session.runner()
					.feed("finalFundamentalEstimate", TFloat64.scalarOf(state.get("finalFundamentalEstimate").getAsDouble()))
					.feed("side", TInt32.scalarOf(state.get("side").getAsInt()))
					.feed("bidSize", TInt32.scalarOf(state.get("bidSize").getAsInt()))
					.feed("askSize", TInt32.scalarOf(state.get("askSize").getAsInt()))
					.feed("spread", TInt32.scalarOf(state.get("spread").getAsInt()))
					.feed("marketHoldings", TInt32.scalarOf(state.get("marketHoldings").getAsInt()))
					.feed("timeTilEnd", TInt32.scalarOf(state.get("timeTilEnd").getAsInt()))
					.feed("bidVector", bidVector)
					.feed("askVector", askVector)
					.feed("transactionHistory", transactionHistory)
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
	
	//Fix this so the length of the array is verified to be < maxLength
	private Tensor<?> jsonToTensor(JsonArray jArray, int maxLength) {
		double[] listdata = new double[maxLength]; 
		if (jArray != null) { 
		   for (int i=0;i<jArray.size();i++){ 
			 listdata[i] = jArray.get(i).getAsDouble();
		   } 
		   for (int i=jArray.size(); i<maxLength; i++) {
			 listdata[i] = 0;
		   }
		}
		
		return Tensors.create(listdata);
	}
}