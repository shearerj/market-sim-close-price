package systemmanager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import data.MultiSimulationObservation;
import entity.agent.Agent;
import entity.infoproc.IP;
import entity.market.Market;

public class MultiSimulationManager {

	public static void main(String... args) {

		File simFolder = new File(".");
		int simNumber = 1;
		switch (args.length) {
		default:
			simNumber = Integer.parseInt(args[1]);
		case 1:
			simFolder = new File(args[0]);
		case 0:
		}

		try {
			MultiSimulationManager manager = new MultiSimulationManager(simFolder, simNumber);
			manager.executeSimulations();
			manager.writeResults();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	private final File simFolder;
	private final int obsNum, numSims;
	private final MultiSimulationObservation observations;

	public MultiSimulationManager(File simFolder, int obsNum) throws FileNotFoundException {
		this.simFolder = simFolder;
		this.obsNum = obsNum;
		this.numSims = new SimulationSpec(new File(simFolder, Consts.SIM_SPEC_FILE)).getSimulationProps().getAsInt(Keys.NUM_SIMULATIONS, 1);
		this.observations = new MultiSimulationObservation();
	}
	
	public void executeSimulations() throws IOException {
		for (int i = 0; i < numSims; i++) {
			Market.nextID = 1;
			Agent.nextID = 1;
			IP.nextID = 1;
			
			SystemManager manager = new SystemManager(simFolder, obsNum	* numSims + i);
			manager.executeEvents();
			observations.addObservation(manager.getObservations());
		}
	}
	
	public void writeResults() throws IOException {
		File results = new File(simFolder, Consts.OBS_FILE_PREFIX + obsNum + ".json");
		observations.writeToFile(results);
	}
	
}
