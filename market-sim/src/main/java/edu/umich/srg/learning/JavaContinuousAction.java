package edu.umich.srg.learning;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
//import no.uib.cipr.matrix.DenseMatrix;

import java.util.Random;

import com.google.gson.JsonArray;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.marketsim.Keys.NbActions;
import edu.umich.srg.marketsim.Keys.NbStates;
import edu.umich.srg.marketsim.Keys.HiddenLayer1;
import edu.umich.srg.marketsim.Keys.HiddenLayer2;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.Keys.ActorWeights;
import edu.umich.srg.marketsim.Keys.IsTraining;
import edu.umich.srg.marketsim.Keys.EpsilonDecay;

public class JavaContinuousAction extends ContinuousAction {
	
	protected final int nbStates;
	protected final int nbActions;
	protected final int hidden1;
	protected final int hidden2;
	
	private final Boolean isTraining;
	private double epsilon;
	
	protected double[][] weight1;
	protected double[][] weight2;
	protected double[][] weight3;
	protected double[][] bias1;
	protected double[][] bias2;
	protected double[][] bias3;
	
	/*
	protected DenseMatrix weight1_iOS;
	protected DenseMatrix weight2_iOS;
	protected DenseMatrix weight3_iOS;
	protected DenseMatrix bias1_iOS;
	protected DenseMatrix bias2_iOS;
	protected DenseMatrix bias3_iOS;
	*/
	
	private OrnsteinUhlenbeckNoise ouNoise;
	protected MatrixLibrary mtxLib;
	
	//private OrnsteinUhlenbeckNoiseiOS ouNoise_iOS;
	//protected MatrixLibraryiOS mtxLib_iOS;

	public JavaContinuousAction(Sim sim, Spec spec, Random rand) {
		super(spec, rand);

	    this.nbStates = spec.get(NbStates.class);
	    this.nbActions = spec.get(NbActions.class);
	    this.hidden1 = spec.get(HiddenLayer1.class);
	    this.hidden2 = spec.get(HiddenLayer2.class);
	    
	    this.isTraining = spec.get(IsTraining.class);
	    this.epsilon = spec.get(EpsilonDecay.class);
	    
	    this.mtxLib = new MatrixLibrary();
	    this.ouNoise = new OrnsteinUhlenbeckNoise(sim,spec,rand);

	    //this.mtxLib_iOS = new MatrixLibraryiOS();
	    //this.ouNoise_iOS = new OrnsteinUhlenbeckNoiseiOS(sim,spec,rand);
	    
	    String weightMtxString = spec.get(ActorWeights.class).iterator().next();
	    JsonParser parser = new JsonParser();
	    JsonObject weightMatrices = (JsonObject) parser.parse(weightMtxString);
	    this.initializeWeights(weightMatrices);
			
	}
		
	public static JavaContinuousAction create(Sim sim, Spec spec, Random rand) {
		return new JavaContinuousAction(sim, spec, rand);
	}
	
	protected void initializeWeights(JsonObject weightMatrices) {
		if(weightMatrices.size() > 0) {
			JsonArray weightJson1 = weightMatrices.get("weightMtx1").getAsJsonArray();
		    JsonArray weightJson2 = weightMatrices.get("weightMtx2").getAsJsonArray();
		    JsonArray weightJson3 = weightMatrices.get("weightMtx3").getAsJsonArray();
		    JsonArray biasJson1 = weightMatrices.get("biasMtx1").getAsJsonArray();
		    JsonArray biasJson2 = weightMatrices.get("biasMtx2").getAsJsonArray();
		    JsonArray biasJson3 = weightMatrices.get("biasMtx3").getAsJsonArray();
		    
		    this.weight1 = mtxLib.jsonToMtx(weightJson1, this.hidden1, this.nbStates);
		    this.weight2 = mtxLib.jsonToMtx(weightJson2, this.hidden2, this.hidden1);
		    this.weight3 = mtxLib.jsonToMtx(weightJson3, this.nbActions, this.hidden2);
		    this.bias1 = mtxLib.jsonToVector(biasJson1, this.hidden1);
		    this.bias2 = mtxLib.jsonToVector(biasJson2, this.hidden2);
		    this.bias3 = mtxLib.jsonToVector(biasJson3, this.nbActions);
		    
		    /*
		    this.weight1_iOS = mtxLib_iOS.jsonToMtx(weightJson1, this.hidden1, this.nbStates);
		    this.weight2_iOS = mtxLib_iOS.jsonToMtx(weightJson2, this.hidden2, this.hidden1);
		    this.weight3_iOS = mtxLib_iOS.jsonToMtx(weightJson3, this.nbActions, this.hidden2);
		    this.bias1_iOS = mtxLib_iOS.jsonToVector(biasJson1, this.hidden1);
		    this.bias2_iOS = mtxLib_iOS.jsonToVector(biasJson2, this.hidden2);
		    this.bias3_iOS = mtxLib_iOS.jsonToVector(biasJson3, this.nbActions);
		    */
		}
	}

	@Override
	public JsonArray getAction(JsonObject state) {
		double[][] stateMtx = mtxLib.jsonToVector(state.get("state0").getAsJsonArray(), this.nbStates);
		/*
		for(int i=0; i < this.nbStates; i++) {
			stateMtx[0][i] = 1;
		}
		*/
		//stateMtx = mtxLib.norm(stateMtx, this.nbStates);
		
		double[][] out1 = mtxLib.nnLinearRelu(stateMtx, this.weight1, this.bias1, 1, this.hidden1, this.nbStates);
		
		double[][] out2 = mtxLib.nnLinearRelu(out1, this.weight2, this.bias2, 1, this.hidden2, this.hidden1);
		
		double[][] out3 = mtxLib.nnLinearTanh(out2, this.weight3, this.bias3, 1, this.nbActions, this.hidden2);

		if(this.isTraining) {
			double[][] noise = ouNoise.ouNoiseMtx(this.nbActions);
			out3 = mtxLib.scaleAdd(out3, noise, this.epsilon, 1, this.nbActions);
			if(this.epsilon > 0 ) {this.epsilon -= 1.0 / this.epsilon;}
		}
		
		out3 = mtxLib.clip(out3, this.nbActions, -1.0, 1.0);
		

		return mtxLib.vectorToJson(out3, this.nbActions);
	}
	
	@Override
	public JsonObject getActionDict(JsonObject state, double finalEstimate) {
		double[][] stateMtx = mtxLib.jsonToVector(state.get("state0").getAsJsonArray(), this.nbStates);
		/*
		for(int i=0; i < this.nbStates; i++) {
			stateMtx[0][i] = 1;
		}
		*/
		//stateMtx = mtxLib.norm(stateMtx, this.nbStates);
		
		double[][] out1 = mtxLib.nnLinearRelu(stateMtx, this.weight1, this.bias1, 1, this.hidden1, this.nbStates);
		
		double[][] out2 = mtxLib.nnLinearRelu(out1, this.weight2, this.bias2, 1, this.hidden2, this.hidden1);
		
		double[][] out3 = mtxLib.nnLinearTanh(out2, this.weight3, this.bias3, 1, this.nbActions, this.hidden2);

		if(this.isTraining) {
			double[][] noise = ouNoise.ouNoiseMtx(this.nbActions);
			out3 = mtxLib.scaleAdd(out3, noise, this.epsilon, 1, this.nbActions);
			if(this.epsilon > 0 ) {this.epsilon -= 1.0 / this.epsilon;}
		}
		
		out3 = mtxLib.clip(out3, this.nbActions, -1.0, 1.0);
		
		JsonObject action = new JsonObject();
		
		action.addProperty("alpha", out3[0][0]);
		action.addProperty("price", this.actionToPrice(finalEstimate));

		return action;
	}
	
}