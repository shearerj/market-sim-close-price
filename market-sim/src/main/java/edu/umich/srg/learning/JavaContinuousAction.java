package edu.umich.srg.learning;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import no.uib.cipr.matrix.DenseMatrix;

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

import edu.umich.srg.learning.MatrixLibrary;

public class JavaContinuousAction extends ContinuousAction {
	
	protected final int nbStates;
	protected final int nbActions;
	protected final int hidden1;
	protected final int hidden2;
	protected MatrixLibrary mtxLib;
	
	private final Boolean isTraining;
	private double epsilon;
	
	protected DenseMatrix weight1;
	protected DenseMatrix weight2;
	protected DenseMatrix weight3;
	protected DenseMatrix bias1;
	protected DenseMatrix bias2;
	protected DenseMatrix bias3;
	
	private OrnsteinUhlenbeckNoise ouNoise;

	public JavaContinuousAction(Sim sim, Spec spec, Random rand) {
		super(spec, rand);

	    this.nbStates = spec.get(NbStates.class);
	    this.nbActions = spec.get(NbActions.class);
	    this.hidden1 = spec.get(HiddenLayer1.class);
	    this.hidden2 = spec.get(HiddenLayer2.class);
	    this.mtxLib = new MatrixLibrary();
	    
	    this.isTraining = spec.get(IsTraining.class);
	    this.epsilon = spec.get(EpsilonDecay.class);

	    this.ouNoise = new OrnsteinUhlenbeckNoise(sim,spec,rand);
	    
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
		}
	}

	@Override
	public JsonArray getAction(JsonObject state) {
		DenseMatrix stateMtx = mtxLib.jsonToVector(state.get("state0").getAsJsonArray(), this.nbStates);
		for (int i=0; i < stateMtx.numColumns(); i++) {
			stateMtx.set(0, i, 1);
		}
		
		//DenseMatrix out1 = new DenseMatrix(1, this.hidden1);
		//DenseMatrix out2 = new DenseMatrix(1, this.hidden2);
		//DenseMatrix out3 = new DenseMatrix(1, this.nbActions);
		
		DenseMatrix out1 = mtxLib.nnLinear(stateMtx, this.weight1, this.bias1);
		out1 = mtxLib.nnReLu(out1);
		
		DenseMatrix out2 = mtxLib.nnLinear(out1, this.weight2, this.bias2);
		out2 = mtxLib.nnReLu(out2);
		
		DenseMatrix out3 = mtxLib.nnLinear(out2, this.weight3, this.bias3);
		out3 = mtxLib.nnTanh(out3);

		if(this.isTraining) {
			DenseMatrix noise = ouNoise.ouNoiseMtx(this.nbActions);
			noise = (DenseMatrix) noise.scale(this.epsilon);
			out3 = (DenseMatrix) out3.add(noise);
			if(this.epsilon > 0 ) {this.epsilon -= 1.0 / this.epsilon;}
		}

		return mtxLib.vectorToJson(out3, this.nbActions);
	}
	
}