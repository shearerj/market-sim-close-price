package edu.umich.srg.learning;

import java.util.List;
import java.util.Random;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.Keys.TensorFlowModelPath;
import edu.umich.srg.marketsim.Keys.MaxVectorDepth;

import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.Tensors;
import org.tensorflow.types.TFloat64;
import org.tensorflow.types.TInt32;
import org.tensorflow.types.TInt64;

public class TensorFlowAction extends ContinuousAction {
	
	private final String tfModelPath;
	private final int maxVectorDepth;

	public TensorFlowAction(Sim sim, Spec spec, Random rand) {
		super(spec, rand);
		this.tfModelPath = spec.get(TensorFlowModelPath.class).iterator().next();
		this.maxVectorDepth = spec.get(MaxVectorDepth.class);
	}

	public static TensorFlowAction create(Sim sim, Spec spec, Random rand) {
		return new TensorFlowAction(sim, spec, rand);
	}

	@Override
	public JsonObject getActionDict(JsonObject state, double finalEstimate) {
		JsonObject action = new JsonObject();
		try(SavedModelBundle savedModelBundle = SavedModelBundle.load(this.tfModelPath, "serve")) {
			Session session = savedModelBundle.session();
			Tensor<?> bidVector = this.jsonToTensor(state.get("bidVector").getAsJsonArray());
			Tensor<?> askVector = this.jsonToTensor(state.get("askVector").getAsJsonArray());
			Tensor<?> transactionHistory = this.jsonToTensor(state.get("transactionHistory").getAsJsonArray());
	
			List<Tensor<?>> order = (List<Tensor<?>>) session.runner()
					.feed("finalFundamentalEstimate", TFloat64.scalarOf(state.get("finalFundamentalEstimate").getAsDouble()))
					.feed("privateBid", TFloat64.scalarOf(state.get("privateBid").getAsDouble()))
					.feed("privateAsk", TFloat64.scalarOf(state.get("privateAsk").getAsDouble()))
					.feed("omegaRatioBid", TFloat64.scalarOf(state.get("omegaRatioBid").getAsDouble()))
					.feed("omegaRatioAsk", TFloat64.scalarOf(state.get("omegaRatioAsk").getAsDouble()))
					.feed("side", TInt32.scalarOf(state.get("side").getAsInt()))
					.feed("bidSize", TInt32.scalarOf(state.get("bidSize").getAsInt()))
					.feed("askSize", TInt32.scalarOf(state.get("askSize").getAsInt()))
					.feed("spread", TInt32.scalarOf(state.get("spread").getAsInt()))
					.feed("marketHoldings", TInt32.scalarOf(state.get("marketHoldings").getAsInt()))
					.feed("contractHoldings", TFloat64.scalarOf(state.get("contractHoldings").getAsDouble()))
					.feed("numTransactions", TInt32.scalarOf(state.get("numTransactions").getAsInt()))
					.feed("timeTilEnd", TInt64.scalarOf(state.get("timeTilEnd").getAsInt()))
					.feed("latency", TInt64.scalarOf(state.get("latency").getAsInt()))
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
	private Tensor<?> jsonToTensor(JsonArray jArray) {
		double[] listdata = new double[this.maxVectorDepth]; 
		if (jArray != null) { 
		   for (int i=0;i<jArray.size();i++){ 
			 listdata[i] = jArray.get(i).getAsDouble();
		   } 
		   for (int i=jArray.size(); i<this.maxVectorDepth; i++) {
			 listdata[i] = 0;
		   }
		}
		
		return Tensors.create(listdata);
	}
}
