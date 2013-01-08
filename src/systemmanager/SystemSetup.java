package systemmanager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Iterator;
import java.util.Random;

import activity.*;
import entity.*;
import event.*;
import model.*;

/**
 * Class to create agent, models, and markets.
 * 
 * Strategies: Sets up and assigns strategies (assumed to follow format of parameter-
 * value pairs separated by underscores: [param]_[value]_...
 * 
 * Sequence of initialization:
 * - generate background arrival times
 * - generate background private valuations
 * - create market models
 *   - create the markets
 * 	 - setup the markets
 *   - add Clear activities, if needed
 *   - create the primary model
 * - create the agents
 *   - duplicate agents for each model
 *   - set up the agents (different if MM vs SM)
 *   - add AgentArrival activities
 * - set agent permission for each model's markets
 * 
 * NOTE: Usage of the "primary_model" setting in the spec file:
 *  1) MODELNAME-CONFIG
 *  2) MODELNAME (will use first config in that model's list to specify primary model)
 * 
 * @author ewah
 */
public class SystemSetup {

	private Log log;
	private SystemData data;
	private EventManager eventManager;
	private SimulationSpec specs;
	
	private Sequence agentIDSequence;
	private Sequence marketIDSequence;
	private Sequence modelIDSequence;
	
	public SystemSetup(SimulationSpec s, EventManager em, SystemData d, Log l) {
		specs = s;
		eventManager = em;
		data = d;
		log = l;
		agentIDSequence = new Sequence(1);
		marketIDSequence = new Sequence(-1);
		modelIDSequence = new Sequence(1);
	}
	
	
	public void setupAll() {
		try {
			// Generate arrival times & private values
			data.backgroundArrivalTimes();
			data.backgroundPrivateValues();

			// Must create market models before agents, so can add the agents
			// then to the appropriate/corresponding markets.
			createQuoter();
			createMarketModels();
			createAgents();

			// Log agent information
			logAgentInfo();

			// Set all agent permissions
			for (Map.Entry<Integer,MarketModel> entry : data.getModels().entrySet()) {
				MarketModel mm = entry.getValue();
				mm.setAgentPermissions();
			}

			// Initial SendToSIP Activity for all markets
			for (Map.Entry<Integer,Market> entry : data.getMarkets().entrySet()) {
				eventManager.createEvent(Consts.DEFAULT_PRIORITY,
							new SendToSIP(entry.getValue(), new TimeStamp(0)));
			}
		} catch (Exception e) {
			System.err.println(this.getClass().getSimpleName() + "::setupAll: error");
			e.printStackTrace();
		}
	}
	
	/**
	 * Create Quoter entity, which enters the system at time 0.
	 */
	public void createQuoter() {
		Quoter iu = new Quoter(0, data, log);
		data.setSIP(iu);
		//eventManager.createEvent(new UpdateNBBO(iu, new TimeStamp(0)));
	}
	
	
	public void createMarketModels() {
		// get information on the primary market model - two possible usages:
		// 1) MODELNAME-CONFIG
		// 2) MODELNAME (use first config in that model's list to specify primary model)
		String[] desc = data.primaryModelDesc.split("[-]+");
		String primaryModel = data.primaryModelDesc;
		String primaryModelConfig;
		if (desc.length > 1) {
			// hard-coded to only take in first two words separated by hyphen, ignore rest
			primaryModel = desc[0];
			primaryModelConfig = desc[1];
		} else {
			// second usage; use first configuration found in the list
			primaryModelConfig = specs.getValue(primaryModel);
			String[] parsed = parseModelConfigList(primaryModelConfig);
			if (parsed.length > 1) {
				// more than one of this model type in the simulation, so pick the first one
				primaryModelConfig = parsed[0];
			}
		}
		
		for (Map.Entry<String, Integer> mdl : data.numModelType.entrySet()) {
			
			int numModelsOfThisType = mdl.getValue();
			String modelType = mdl.getKey();
			
			// log before the markets are created
			log.log(Log.INFO, "Models: " + numModelsOfThisType + " " + modelType);
			
			// Parse the comma separated types of agents
			String[] configs = parseModelConfigList(specs.getValue(modelType));
			
			for (int i = 0; i < numModelsOfThisType; i++) {
				ObjectProperties p = getEntityProperties(modelType, i);
				
				// create market model & determine its configuration
				int modelID = modelIDSequence.increment();
				p.put(Consts.MODEL_CONFIG_KEY, configs[i]);
				MarketModel model = ModelFactory.createModel(modelType, modelID, p, data);		
				data.addModel(model);
				log.log(Log.INFO, model.getFullName() + ": " + model + " " + p);
				
				// create markets in the model
				for (Iterator<MarketObjectPair> it = model.getMarketConfig().iterator(); it.hasNext(); ) {
					MarketObjectPair mop = it.next();
					int mID = marketIDSequence.decrement();
					
					ObjectProperties mp = (ObjectProperties) mop.getObject();
					String mtype = mop.getMarketType();
					Market market = MarketFactory.createMarket(mtype, mID, data, mp, log);
					market.linkModel(model.getID());
					data.addMarket(market);
					model.getMarketIDs().add(mID);
					log.log(Log.DEBUG, market.toString() + ": " + mp);
					
					// Check if is call market, then initialize clearing sequence
					if (market instanceof CallMarket) {
						Activity clear = new Clear(market, market.getNextClearTime());
						eventManager.createEvent(Consts.CALL_CLEAR_PRIORITY, clear);
					}
					log.log(Log.INFO, "Markets: " + market.getType() + ", " + market);
					
					data.marketToModel.put(mID, model.getID());
				}
				
				// set up primary model
				if (mdl.getKey().equals(primaryModel)) {
					// check if primary model configuration is contained in the specs file
					if (Arrays.asList(configs).contains(primaryModelConfig)) {
						// check that the current model type matches
						if (configs[i].equals(primaryModelConfig)) {
							if (data.primaryModel == null) {
								// only initialize primary model once
								data.primaryModel = model;
							}
						}
					} else {
						String s = this.getClass().getSimpleName() + "::" + 
								"createMarketModels: " + "primary model type not found in " +
								"configuration list for " + modelType;
						log.log(Log.ERROR, s);
						System.err.println(s);
					}
				}
			}
		}
		
		// log primary model
		log.log(Log.INFO, "Primary model: " + primaryModel + "-" + primaryModelConfig);
		
		// set the model to market list
		for (Map.Entry<Integer,MarketModel> mdl : data.models.entrySet()) {
			data.modelToMarketList.put(mdl.getKey(), mdl.getValue().getMarketIDs());
		}
	}
	
	/**
	 * Parse list of model configruations from simulation specifications file.
	 * 
	 * @param modelConfigList
	 * @return
	 */
	private String[] parseModelConfigList(String modelConfigList) {
		String[] configs = null;
		if (modelConfigList != null) {
			if (modelConfigList.endsWith(",")) {
				// remove any extra appended commas
				modelConfigList = modelConfigList.substring(0, modelConfigList.length() - 1);
			}
			configs = modelConfigList.split("[,]+");
		}
		return configs;
	}
	
	
	/**
	 * Creates the agents for each model. Each model has the same number of each agent type (which is
	 * specified in the simulation spec file).
	 */
	private void createAgents() {
		boolean initSMValues = false;
		ArrayList<TimeStamp> arrivals = new ArrayList<TimeStamp>();
		ArrayList<Integer> values = new ArrayList<Integer>();

		boolean initValues = false;
		ArrayList<Long> seeds = new ArrayList<Long>();	
		Random rand = new Random();

		for (Map.Entry<Integer,MarketModel> entry : data.getModels().entrySet()) {
			int modelID = entry.getKey();
			MarketModel model = entry.getValue();

			// reset logID to 0 to increment through all created agents
			int logID = 0;

			for (Map.Entry<String, Integer> ag : data.numAgentType.entrySet()) {
	
				String agentType = ag.getKey();
				int numAgents = ag.getValue();
				ArrayList<Integer> assignment = assignAgentsToMarkets(numAgents, model.getMarketIDs());
	
				for (int i = 0; i < numAgents; i++) {
					ObjectProperties ap = getEntityProperties(agentType, i);
					// Set initialization values for all agents
					if (!initValues) {
						long seed = rand.nextLong();
						seeds.add(seed);
					}	
					ap.put("seed", seeds.get(logID).toString());
					int agentID = agentIDSequence.increment();
					Agent agent;

					// create agent & events
					if (!Arrays.asList(Consts.SMAgentTypes).contains(agentType)) {
						// Multi-market agent
						agent = AgentFactory.createMMAgent(agentType, agentID, modelID, data, ap, log);
						
						TimeStamp ts = agent.getArrivalTime();
						if (agent instanceof MMAgent) {
							// Agent is in multiple markets (an extra check)
							eventManager.createEvent(new AgentArrival(agent, ts));
							eventManager.createEvent(new AgentDeparture(agent, data.simLength));
						}
					} else {
						// Single market agent
						int mktID = assignment.get(i);

						if (!initSMValues) {
							// set up arrival time/fundamental for background agents
							TimeStamp t = data.nextArrival();
							int pv = data.nextPrivateValue();
							ap.put("arrivalTime", t.toString());
							ap.put("fundamental", (new Integer(pv)).toString());
							arrivals.add(t);
							values.add(pv);
						} else {
							ap.put("arrivalTime", arrivals.get(i).toString());
							ap.put("fundamental", values.get(i).toString());
						}
						agent = AgentFactory.createSMAgent(agentType, agentID, modelID, data, ap, log, mktID);

						TimeStamp ts = agent.getArrivalTime();
						if (agent instanceof SMAgent) {
							// Agent is in single market (an extra check)
							Market mkt = ((SMAgent) agent).getMarket();
							eventManager.createEvent(new AgentArrival(agent, mkt, ts));
							eventManager.createEvent(new AgentDeparture(agent, mkt, data.simLength));		
						}
					}
					data.addAgent(agent);
					agent.setLogID(logID++);
					model.linkAgent(agentID);
					// check if in a role, if so keep track of role agent IDs
					if (Arrays.asList(Consts.roles).contains(agentType)) {
						data.roleAgentIDs.add(agentID);
					}
					log.log(Log.DEBUG, agent.toString() + ": " + ap);
				}
				log.log(Log.INFO, "Agents: " + numAgents + " " + agentType);
			}
			// values will be initialized after first model has been created
			initSMValues = true;
			initValues = true;
		}
	}

	/**
	 * Given a number of agents and a number of market IDs, splits them as evenly as possible. Returns an
	 * array of length numAgents, each element being one of the valid marketIDs. The distribution is
	 * as even as possible.
	 *
	 * @param numAgents
	 * @param mktIDs
	 */
	private ArrayList<Integer> assignAgentsToMarkets(int numAgents, ArrayList<Integer> mktIDs) {
		ArrayList<Integer> assignment = new ArrayList<Integer>();
		
		int remain = numAgents % mktIDs.size();
		int num = (numAgents - remain) / mktIDs.size();
		for (int i = 0; i < num; i++) {
			for (Iterator<Integer> it = mktIDs.iterator(); it.hasNext(); ) {
				assignment.add(it.next());
			}
		}
		for (int i = 0; i < remain; i++) {
			assignment.add(mktIDs.get(i));
		}		

		return assignment;
	}

	/**
	 * Logs agent information (only for primary model to avoid redundancy).
	 */
	public void logAgentInfo() {
		ArrayList<Integer> ids = data.getAgentIDs(); // or data.getPrimaryModel().getAgentIDs()
		for (Iterator<Integer> it = ids.iterator(); it.hasNext(); ) {
			Agent ag = data.getAgent(it.next());
			
			// print arrival times
			String s = ag.toString() + "::" + ag.getType() + "::";
			s += "arrivalTime=" + ag.getArrivalTime().toString();
			
			// print private value if exists 
			if (ag instanceof ZIAgent) {
				s += ", pv=" + ((ZIAgent) ag).getPrivateValue();
			}
			log.log(Log.INFO, s);
		}
	}
	
	/**
	 * Gets properties for an entity. May overwrite default EntityProperties set in
	 * Consts. If the entity type indicates that the entity is a player in a role, 
	 * this method parses the strategy, if any, in the simulation spec file.
	 * The index is used to select the player from the list in the spec file.
	 *
	 * @param type	Entity type
	 * @param idx	index of the role, -1 otherwise
	 * @return EntityProperties
	 */
	public ObjectProperties getEntityProperties(String type, int idx) {
		if (specs.getRoleStrategies().containsKey(type) && idx >= 0) {
			ObjectProperties p = new ObjectProperties(Consts.getProperties(type));
			
			ArrayList<String> players = (ArrayList<String>) specs.getRoleStrategies().get(type);
			String strategy = players.get(idx);
			p.put("strategy", strategy);
			
			// Check that strategy is not blank
			if (!strategy.equals("") && !type.equals("DUMMY")) {
				String[] stratParams = strategy.split("[_]+");
				if (stratParams.length % 2 != 0) {
					log.log(Log.ERROR, "getEntityProperties: error parsing strategy " + stratParams);
					return null;
				}
				for (int j = 0; j < stratParams.length; j += 2) {
					p.put(stratParams[j], stratParams[j+1]);
				}
			}
			log.log(Log.INFO, type + ": " + p);
			return p;
			
		} else {
			return new ObjectProperties(Consts.getProperties(type));
		}
	}

}
