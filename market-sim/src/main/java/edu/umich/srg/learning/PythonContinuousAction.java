package edu.umich.srg.learning;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import edu.umich.srg.distributions.Uniform;
import edu.umich.srg.distributions.Uniform.ContinuousUniform;
import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.marketsim.Keys.PythonModelPath;
import edu.umich.srg.marketsim.Keys.BenchmarkParamPath;
import edu.umich.srg.marketsim.Keys.GreatLakesJobNumber;
import edu.umich.srg.marketsim.Keys.NbActions;
import edu.umich.srg.marketsim.Keys.NbStates;
import edu.umich.srg.marketsim.Keys.IsTraining;

public class PythonContinuousAction extends ContinuousAction {
	
	private ContinuousUniform actionsToSubmit;
	private Random rand;
	private double alpha;
	
	private final String pythonModelPath;
	private final String benchmarkParamPath;
	private final int glJobNum;
	private final int nbStates;
	private final int nbActions;
	private final Boolean isTraining;

	public PythonContinuousAction(Spec spec, Random rand) {
		super(spec, rand);
		this.rand = rand;
		this.actionsToSubmit = Uniform.closedOpen(-1.0, 1.0);
		this.alpha = 0;
		this.pythonModelPath =spec.get(PythonModelPath.class);
	    this.benchmarkParamPath =spec.get(BenchmarkParamPath.class);
	    this.glJobNum = spec.get(GreatLakesJobNumber.class);
	    this.nbStates = spec.get(NbStates.class);
	    this.nbActions = spec.get(NbActions.class);
	    this.isTraining = spec.get(IsTraining.class);
			
	}
		
	public static PythonContinuousAction create(Spec spec, Random rand) {
		return new PythonContinuousAction(spec, rand);
	}

	@Override
	public JsonArray getAction(JsonObject state) {
		JsonArray action = new JsonArray();
		
		try {
	    	String statePath;
	    	if (this.glJobNum >= 0) {
	    		statePath = "temp_state_" + glJobNum + ".json";
	    	}
	    	else {
	    		statePath = "temp_state.json";
	    	}
			FileWriter stateFile = new FileWriter(statePath,false);
			stateFile.write(state.toString());
			stateFile.close();
			
			//double toSubmit = PythonPolicyAction();
			
			int training  = 0;
			if (this.isTraining) {
				training = 1;
			} 
			ProcessBuilder pb = new ProcessBuilder("python3","action.py","-p",""+this.benchmarkParamPath,
					"-f",""+statePath,"-m",""+this.pythonModelPath,"-s",""+this.nbStates,
					"-a",""+this.nbActions,"-t",""+training);
			Process p = pb.start();
			 
			BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
			this.alpha = new Double(in.readLine()).doubleValue();
	        
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
	        this.alpha = this.actionsToSubmit.sample(this.rand);
		}
		
		action.add(this.alpha);
		return action;
	}
}