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
			//System.out.println(TFloat64.scalarOf(state.get("finalFundamentalEstimate").getAsDouble()));
			System.out.println(state.get("finalFundamentalEstimate").getAsDouble());
	
			List<Tensor<?>> order = (List<Tensor<?>>) session.runner()
					.feed("serving_default_finalFundamentalEstimate", TFloat64.scalarOf(state.get("finalFundamentalEstimate").getAsDouble()))
					.feed("serving_default_privateBid", TFloat64.scalarOf(state.get("privateBid").getAsDouble()))
					.feed("serving_default_privateAsk", TFloat64.scalarOf(state.get("privateAsk").getAsDouble()))
					.feed("serving_default_omegaRatioBid", TFloat64.scalarOf(state.get("omegaRatioBid").getAsDouble()))
					.feed("serving_default_omegaRatioAsk", TFloat64.scalarOf(state.get("omegaRatioAsk").getAsDouble()))
					.feed("serving_default_side", TInt32.scalarOf(state.get("side").getAsInt()))
					.feed("serving_default_bidSize", TInt32.scalarOf(state.get("bidSize").getAsInt()))
					.feed("serving_default_askSize", TInt32.scalarOf(state.get("askSize").getAsInt()))
					.feed("serving_default_spread", TInt32.scalarOf(state.get("spread").getAsInt()))
					.feed("serving_default_marketHoldings", TInt32.scalarOf(state.get("marketHoldings").getAsInt()))
					.feed("serving_default_contractHoldings", TFloat64.scalarOf(state.get("contractHoldings").getAsDouble()))
					.feed("serving_default_numTransactions", TInt32.scalarOf(state.get("numTransactions").getAsInt()))
					.feed("serving_default_timeTilEnd", TInt64.scalarOf(state.get("timeTilEnd").getAsInt()))
					.feed("serving_default_latency", TInt64.scalarOf(state.get("latency").getAsInt()))
					.feed("serving_default_bidVector", bidVector)
					.feed("serving_default_askVector", askVector)
					.feed("serving_default_transactionHistory", transactionHistory)
					.fetch("PartitionedCall",0)
					.run();
			
			System.out.println(order.get(0).rawData().asDoubles().getDouble(0));
			action.addProperty("price", order.get(0).rawData().asDoubles().getDouble(0));
			//action.addProperty("side", order.get(0).rawData().asInts().getInt(1));
			//action.addProperty("size", order.get(0).rawData().asInts().getInt(2));
			//action.addProperty("side", order.get(1).rawData().asInts().getInt(0));
			//action.addProperty("size", order.get(2).rawData().asInts().getInt(2));

			return action;

		}
	}
	
	//Fix this so the length of the array is verified to be < maxLength
	private Tensor<TFloat64> jsonToTensor(JsonArray jArray) {
		double[] listdata = new double[this.maxVectorDepth]; 
		if (jArray != null) { 
		   for (int i=0;i<jArray.size();i++){ 
			 listdata[i] = jArray.get(i).getAsDouble();
		   } 
		   for (int i=jArray.size(); i<this.maxVectorDepth; i++) {
			 listdata[i] = 0;
		   }
		}
		
		return TFloat64.vectorOf(listdata);
	}
}
