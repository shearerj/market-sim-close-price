package systemmanager;

import java.util.ArrayList;
import java.util.Collection;

import utils.RandPlus;
import model.MarketModel;
import model.MarketModelFactory;
import data.FundamentalValue;
import data.ModelProperties;
import data.ObjectProperties;
import event.EventManager;

public class Setup {

	protected final SimulationSpec2 spec;
	protected final EventManager manager;

	public Setup(SimulationSpec2 spec, EventManager manager) {
		this.spec = spec;
		this.manager = manager;
	}

	public void setup() {
		ObjectProperties simProps = spec.getSimulationProperties();

		RandPlus rand = new RandPlus(simProps.getAsLong(
				SimulationSpec2.RAND_SEED, System.currentTimeMillis()));

		FundamentalValue fundamental = new FundamentalValue(
				simProps.getAsDouble(SimulationSpec2.FUNDAMENTAL_KAPPA),
				simProps.getAsInt(SimulationSpec2.FUNDAMENTAL_MEAN),
				simProps.getAsDouble(SimulationSpec2.FUNDAMENTAL_SHOCK_VAR),
				new RandPlus(rand.nextLong()));

		MarketModelFactory modelFactory = new MarketModelFactory(
				spec.getBackgroundAgents(), fundamental, new RandPlus(
						rand.nextLong()));

		Collection<MarketModel> models = new ArrayList<MarketModel>();
		for (ModelProperties props : spec.getModels())
			models.add(modelFactory.createModel(props));

		for (MarketModel model : models)
			model.scheduleActivities(manager);
	}

}
