package edu.umich.srg.learning;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.Keys.TensorFlowModelPath;
import edu.umich.srg.marketsim.Keys.MaxVectorDepth;
import edu.umich.srg.marketsim.Keys.AdditionalActions;

import org.tensorflow.Operation;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Session.Runner;
import org.tensorflow.Tensor;
import org.tensorflow.types.TFloat64;
import org.tensorflow.types.TInt32;
import org.tensorflow.types.TInt64;
import org.tensorflow.types.TBool;

public class TensorFlowAction extends ContinuousAction {
	
	private final String tfModelPath;
	private final int maxVectorDepth;
	private final Iterable<String> addActions;
	
	private final SavedModelBundle savedModelBundle;
	//private final Session session;
	//private Runner tfGraph;

	public TensorFlowAction(Sim sim, Spec spec, Random rand) {
		super(spec, rand);
		
		this.tfModelPath = spec.get(TensorFlowModelPath.class);
		this.maxVectorDepth = spec.get(MaxVectorDepth.class);
		this.addActions = spec.get(AdditionalActions.class);
		
		SavedModelBundle savedModelBundle = SavedModelBundle.load(this.tfModelPath, "serve");
		this.savedModelBundle = savedModelBundle;
		/*try(SavedModelBundle savedModelBundle = SavedModelBundle.load(this.tfModelPath, "serve")) {
			this.savedModelBundle = savedModelBundle;
			//this.session = savedModelBundle.session();
		}*/
			
	}

	public static TensorFlowAction create(Sim sim, Spec spec, Random rand) {
		return new TensorFlowAction(sim, spec, rand);
	}

	@Override
	public JsonObject getActionDict(JsonObject state, double finalEstimate) {
		JsonObject action = new JsonObject();
		//try(SavedModelBundle savedModelBundle = SavedModelBundle.load(this.tfModelPath, "serve")) {
			//Session session = savedModelBundle.session();
		
		Session session = savedModelBundle.session();
		
		// Determine if the name of the output op of the graph is "StatefulPartitionedCall" or "PartitionedCall"
		Iterator<Operation> ops = savedModelBundle.graph().operations();
		Operation op;
		String outputOpName = null;
		while(ops.hasNext()) {
			op = ops.next();
			if (op.numOutputs() >=3 && op.name().endsWith("PartitionedCall")) {
				outputOpName = op.name();
			}
		}
		
		Tensor bidVector = this.jsonToTensor(state.get("bidVector").getAsJsonArray());
		Tensor askVector = this.jsonToTensor(state.get("askVector").getAsJsonArray());
		Tensor transactionHistory = this.jsonToTensor(state.get("transactionHistory").getAsJsonArray());
		
		Iterator<String> itActions;

		//List<Tensor> order;
		
		Runner tfGraph = session.runner()
				.feed("serving_default_isTraining", TBool.scalarOf(state.get("isTraining").getAsBoolean()))
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
				.feed("serving_default_timeSinceLastTrade", TInt64.scalarOf(state.get("timeSinceLastTrade").getAsInt()))
				.feed("serving_default_timeTilEnd", TInt64.scalarOf(state.get("timeTilEnd").getAsInt()))
				.feed("serving_default_latency", TInt64.scalarOf(state.get("latency").getAsInt()))
				.feed("serving_default_bidVector", bidVector)
				.feed("serving_default_askVector", askVector)
				.feed("serving_default_transactionHistory", transactionHistory);
		
		tfGraph = tfGraph
				.fetch(outputOpName,0)
				.fetch(outputOpName,1)
				.fetch(outputOpName,2);
		
		itActions = this.addActions.iterator();
		//Start index at 3 because of price, side, and size
		int i =3;
		while (itActions.hasNext()) {
			itActions.next();
			tfGraph = tfGraph.fetch(outputOpName,i);
			i++;
		}
		
		List<Tensor> order = tfGraph.run();
		
		action.addProperty("price", order.get(0).asRawTensor().data().asDoubles().getDouble(0));
		action.addProperty("side", order.get(1).asRawTensor().data().asInts().getInt(0));
		action.addProperty("size", order.get(2).asRawTensor().data().asInts().getInt(0));
		
		itActions = this.addActions.iterator();
		//Start index at 3 because of price, side, and size
		i = 3;
		while (itActions.hasNext()) {
			action.addProperty(itActions.next(), order.get(i).asRawTensor().data().asFloats().getFloat(0));
			i++;
		}

		return action;

		//}
	}
	
	//Fix this so the length of the array is verified to be < maxLength
	private Tensor jsonToTensor(JsonArray jArray) {
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
