package systemmanager;

import data.*;
import event.*;
import logger.Logger;
import model.*;
import entity.*;
import activity.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

import systemmanager.Consts.AgentType;
import systemmanager.Consts.ModelType;

/**
 * SYSTEMSETUP
 * 
 * Class to create agent, models, and markets.
 * 
 * There are two use cases: EGTA analysis and Market models simulation.
 * 	I) EGTA analysis
 * 		- In this use case, only ONE market model will be created. This is specified
 * 		  by the primary market model in the spec file.
 * 		- Model-level agents are not created. Both environment agents & players will
 * 		  be created.
 * 	II) Market models simulation
 * 		- In this case, multiple market models can be specified.
 * 		- Anything in the "assignment" section of the spec file will be ignored.
 * 		- No players will be created. Both environment and model-level agents will be
 * 		  created.
 *      - Set of types of model-level agents does NOT include any agents specified in
 *        the configuration section of the spec file (i.e. environment agents).
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
 * - create the agents
 * 	 - environment agents, model-level agents, or players 
 *   - common random numbers for agents in each market model
 *   - agent-specific parameters are added to ObjectProperties
 *   - add AgentArrival activities
 * - set agent permission for each model's markets
 * 
 * NOTE: Usage of the primary model setting in the spec file:
 *  1) MODELNAME-CONFIG
 *  2) MODELNAME (will use first config in that model's list to specify primary model)
 * 
 * @author ewah
 */
public class SystemSetup {

	private SystemData data;
	private EventManager eventManager;
	private SimulationSpec specs;
	
	private Sequence agentIDSequence;
	private Sequence marketIDSequence;
	private Sequence modelIDSequence;
	private Sequence ipIDSequence;
	
	private HashMap<AgentPropsPair, ArrayList<Integer>> logIDs;
	private HashMap<AgentPropsPair, ArrayList<Long>> seeds;
	private HashMap<AgentPropsPair, ArrayList<TimeStamp>> arrivals;
	private HashMap<Integer, MarketModel> modelMap;		// hashed by model ID
	private HashMap<AgentPropsPair, Integer> allNonPlayers;
	private HashMap<AgentPropsPair, ArrivalTime> arrivalGenerators;
	
	private HashMap<String, Integer> playerNumbers;
	private HashMap<String, ArrivalTime> playerArrivalGenerators;
	private HashMap<String, ArrayList<ObjectProperties>> playerStrategies;
	
	/**
	 * Constructor
	 * @param s
	 * @param em
	 * @param d
	 * @param l
	 */
	public SystemSetup(SimulationSpec s, EventManager em, SystemData d) {
		specs = s;
		eventManager = em;
		data = d;
		agentIDSequence = new Sequence(1);
		marketIDSequence = new Sequence(-1);
		modelIDSequence = new Sequence(1);
		ipIDSequence = new Sequence(0);
		
		logIDs = new HashMap<AgentPropsPair, ArrayList<Integer>>();
		seeds = new HashMap<AgentPropsPair, ArrayList<Long>>();
		arrivals = new HashMap<AgentPropsPair, ArrayList<TimeStamp>>();
		modelMap = new HashMap<Integer, MarketModel>();
		allNonPlayers = new HashMap<AgentPropsPair, Integer>();
		arrivalGenerators = new HashMap<AgentPropsPair, ArrivalTime>();
		playerNumbers = new HashMap<String, Integer>();
		playerArrivalGenerators = new HashMap<String, ArrivalTime>();
		playerStrategies = new HashMap<String, ArrayList<ObjectProperties>>();
	}
	
	
	public void setupAll() {
		try {
			// Generate private values
			data.globalFundamentalValues();
			data.EGTA = (data.getNumPlayers() > 0); // TODO Should have one flag for EGTA
			
			// Must create market models before agents, so can add the agents
			// then to the appropriate/corresponding markets.

			Logger.log(Logger.INFO, "------------------------------------------------");
			Logger.log(Logger.INFO, "            Creating MARKET MODELS");
			createMarketModels();
			Logger.log(Logger.INFO, "------------------------------------------------");
			Logger.log(Logger.INFO, "            Creating AGENTS");
			createAllAgents(); // TODO Move to market Models

			// Log agent information
			logAgentInfo(); // TODO move to agent creation...
			Logger.log(Logger.INFO, "------------------------------------------------");
			Logger.log(Logger.INFO, " ");

			// Initial SendToSIP Activity for all markets
			for (Market mkt : data.getMarkets().values()) { // Move to market Models
				eventManager.addActivity(new SendToSIP(mkt, new TimeStamp(0)));
			}
		} catch (Exception e) {
			System.err.println(this.getClass().getSimpleName() + "::setupAll: error");
			e.printStackTrace();
		}
	}
	
	/**
	 * Create SIP entity, which enters the system at time 0.
	 */
	public void createSIP() {
		SIP iu = new SIP(0, data);
		data.setSIP(iu);
	}
	
	/**
	 * Creates all market models
	 */
	public void createMarketModels() {
		
		// If EGTA use case, then only configure the primary model
		if (data.isEGTAUseCase()) {
			
			// get information on the primary market model: (MODELNAME)
			String[] desc = data.primaryModelDesc.split("[-]+");
			try {
				ModelType primaryModelType = ModelType.valueOf(desc[0]);
				// always use default configuration of this market model
				MarketModel primary = createModel(primaryModelType, "");
				data.primaryModel = primary;
				Logger.log(Logger.INFO, "Primary model: " + primaryModelType + "-(default)");
				
			} catch (Exception e) {
				if (! Consts.ModelType.contains(desc[0])) {
					System.err.println(this.getClass().getSimpleName() + 
							"::createMarketModels: invalid primary market model type");
				}
				System.err.println(e.toString());
			}
			
		} else {
			// No players means market models simulation; no primary model (set as null)
			data.primaryModel = null;
			
			// Create by model type
			for (ModelType modelType : data.numModelType.keySet()) {
				
				int numModelsOfThisType = data.numModelType.get(modelType);
				Logger.log(Logger.INFO, "Models: " + numModelsOfThisType + " " + modelType);
				
				// Parse the comma separated types of agents (list of configuration strings)
				// TODO Put this in config
				String[] configs = parseModelConfigList(specs.getValue(modelType));
				
				for (int i = 0; i < numModelsOfThisType; i++) {
					createModel(modelType, configs[i]);
				}
			}
		}
	}
	
	
	/**
	 * Parse list of model configurations from simulation specifications file.
	 * 
	 * @param modelConfigList
	 * @return
	 */
	private String[] parseModelConfigList(String modelConfigList) {
		String[] configs = null;
		if (modelConfigList != null) {
			if (modelConfigList.endsWith(",")) {
				// remove any extra appended commas
				modelConfigList = modelConfigList.substring(0, modelConfigList.length()-1);
			}
			configs = modelConfigList.split("[,]+");
		}
		return configs;
	}
	
	/**
	 * Given model type & configuration string, creates the market model.
	 * 
	 * @param modelType
	 * @param configuration
	 * @return model
	 */
	private MarketModel createModel(ModelType modelType, String configuration) {
		ObjectProperties p = new ObjectProperties(Consts.getProperties(modelType));
		
		// create market model & determine its configuration
		int modelID = modelIDSequence.increment();
		p.put(Consts.MODEL_CONFIG_KEY, configuration);
		MarketModel model = ModelFactory.createModel(modelType, modelID, p, data, ipIDSequence.increment());		
		data.addModel(model);
		Logger.log(Logger.INFO, model.getFullName() + ": " + model + " " + p);
		createMarketsInModel(model);
		return model;
	}
	
	
	/**
	 * Create markets in the given market model.
	 * @param model
	 */
	private void createMarketsInModel(MarketModel model) {
		// create markets in the model
		for (MarketObjectPair mop : model.getMarketConfig()) {
			int mID = marketIDSequence.decrement();
			
			ObjectProperties mp = (ObjectProperties) mop.getObject();
			String mtype = mop.getMarketType();
			Market market = MarketFactory.createMarket(mtype, mID, data, mp, model, ipIDSequence.increment());
			ipIDSequence.increment(); // must increment again as there are 2 IP's created
			market.linkModel(model.getID());
			data.addMarket(market);
			model.getMarketIDs().add(mID);
			
			// Check if is call market, then initialize clearing sequence
			if (market instanceof CallMarket) {
				eventManager.addActivity(new Clear(market, market.getNextClearTime()));
			}
			Logger.log(Logger.INFO, "Markets: " + market.getType() + ", " + market);
			
			data.marketIDModelIDMap.put(mID, model.getID());
		}
	}
	
	
	/**
	 * Creates the agents for each model. Each model has the same number of each 
	 * agent type (which is specified in the simulation spec file).
	 */
	private void createAllAgents() {
		
		if (data.isEGTAUseCase()) {
			// EGTA - create agents only for primary model
			// No model-level agents created in this use case
			modelMap.put(data.getPrimaryModel().getID(), data.getPrimaryModel());
			
			// set up their players & their arrivals
			for (AgentPropsPair app : data.getPlayerMap().keySet()) {
				String type = app.getAgentType().toString();
				ObjectProperties o = app.getProperties();
				int n = data.getPlayerMap().get(app);
				
				if (playerNumbers.containsKey(type)) {
					playerNumbers.put(type, playerNumbers.get(type) + n);
					playerStrategies.get(type).addAll(Collections.nCopies(n, o));
					
				} else {
					playerNumbers.put(type, n);
					playerArrivalGenerators.put(type, 
							new ArrivalTime(new TimeStamp(0), data.arrivalRate));
					playerStrategies.put(type, new ArrayList<ObjectProperties>(
							Collections.nCopies(n, o)));
				}
			}
			
		} else {
			// MARKET MODELS SIMULATION - create environment & model agents
			// Environment agents are present in EVERY market model
			modelMap.putAll(data.getModels());
			allNonPlayers.putAll(data.getModelAgentMap());
		}
				
		// add environment agents & set up their arrivals
		allNonPlayers.putAll(data.getEnvAgentMap());
		for (AgentPropsPair app : allNonPlayers.keySet()) {
			// check if arrival rate already described for this AgentPropertiesPair
			double rate = app.getProperties().getAsDouble(Agent.ARRIVALRATE_KEY, data.arrivalRate);
			arrivalGenerators.put(app, new ArrivalTime(new TimeStamp(0), rate));
		}

		// TODO pull out arrivals for future reference
		
		// generate logIDs, random number generator seeds, & arrivals for non-players
		Random rand = new Random();
		int id = data.getNumPlayers() + 1;
		for (AgentPropsPair prop: allNonPlayers.keySet()) {
			ArrayList<Integer> ids = new ArrayList<Integer>();
			ArrayList<Long> sd = new ArrayList<Long>();
			ArrayList<TimeStamp> arr = new ArrayList<TimeStamp>();
			
			// iterate through # of agents specified by this AgentPropsPair
			for (int i = 0; i < allNonPlayers.get(prop); i++) {
				ids.add(id++);
				sd.add(rand.nextLong());
				arr.add(arrivalGenerators.get(prop).next());
			}
			logIDs.put(prop, ids);
			seeds.put(prop, sd);
			arrivals.put(prop, arr);
		}
		
		// create all agents
		for (MarketModel model : modelMap.values()) {
			Logger.log(Logger.INFO, "MODEL: " + model.getFullName() + " agent types:");
			if (data.isEGTAUseCase()) {
				createPlayersForModel(model);
			}
			createNonPlayersForModel(model);
		}
	}


	/**
	 * Creates non-player agents in the specified model.
	 * @param model
	 */
	private void createNonPlayersForModel(MarketModel model) {
		try {
			for (AgentPropsPair ap : allNonPlayers.keySet()) {
				int numAg = allNonPlayers.get(ap);
				ArrayList<Integer> assignMktIDs = assignAgToMkts(numAg, model.getMarketIDs());

				// check if (1) is model agent, (2) present in this model OR if environment agent
				if ((data.getModelAgentMap().containsKey(ap) && 
						data.getModelAgentByModel(model.getID()).contains(ap)) ||
						data.getEnvAgentMap().containsKey(ap)) {
					
					for (int i = 0; i < numAg; i++) {
						// create copy of ObjectProperties in case modify it
						ObjectProperties op = new ObjectProperties(ap.getProperties());
						if (data.isSMAgent(ap.getAgentType())) {
							// must assign market if single-market agent
							op.put(SMAgent.MARKETID_KEY, assignMktIDs.get(i).toString());
						}
						Agent ag = createAgent(model, new AgentPropsPair(ap.getAgentType(), op),
								seeds.get(ap).get(i), arrivals.get(ap).get(i));
						ag.setLogID(logIDs.get(ap).get(i));
					}
					Logger.log(Logger.INFO, "Agents: " + numAg + " " + ap.getAgentType() + " " +
							ap.getProperties());
				}
			}
		} catch (Exception e) {
			System.err.println(this.getClass().getSimpleName() + 
					"::createNonPlayersForModel: error");
			e.printStackTrace();
			System.exit(1);
		}
	}

	
	/**
	 * Create players for the specified model.
	 * @param model
	 */
	private void createPlayersForModel(MarketModel model) {
		// only creates players in primary model
		try {
			for (String type : playerNumbers.keySet()) {
				int numAg = playerNumbers.get(type);
				ArrayList<Integer> assignMktIDs = assignAgToMkts(numAg, model.getMarketIDs());
				
				Random rand = new Random();
				for (int i = 0; i < numAg; i++) {	
					// create copy of ObjectProperties in case modify it
					AgentType agType = AgentType.valueOf(type);
					ObjectProperties op = new ObjectProperties(playerStrategies.get(type).get(i));
					if (data.isSMAgent(agType)) {
						// must assign market if single-market agent
						op.put(SMAgent.MARKETID_KEY, assignMktIDs.get(i).toString());
					}
					Agent ag = createAgent(model, new AgentPropsPair(agType, op),
							rand.nextLong(), playerArrivalGenerators.get(type).next());
					ag.setLogID(ag.getID()); 	// set player ID = log ID
					data.addPlayer(ag);			// add player to SystemData
				}
				Logger.log(Logger.INFO, "Players: " + numAg + " " + type);
			}
		} catch (Exception e) {
			System.err.println(this.getClass().getSimpleName() + 
					"::createPlayersForModel: error");
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Create an agent with the specified properties & type in the given market model.
	 *
	 * @param model
	 * @param ap
	 * @param seed
	 * @param arr
	 * @return
	 */
	private Agent createAgent(MarketModel model, AgentPropsPair ap, Long seed, TimeStamp arr) { 
		AgentType agType = ap.getAgentType();
		ObjectProperties p = ap.getProperties();
		
		p.put(Agent.RANDSEED_KEY, seed.toString());
		int agID = agentIDSequence.increment();
		if (data.isSMAgent(agType)) {
			// must assign market if single-market agent
			p.put(Agent.ARRIVAL_KEY, arr.toString());
			p.put(Agent.FUNDAMENTAL_KEY, model.getFundamentalAt(arr).toString());				
		}
		Agent agent = AgentFactory.createAgent(agType, agID, model.getID(), data, p);
		
		createInitialAgentEvents(agent);
		data.addAgent(agent);
		model.addAgent(agent);
		model.linkAgent(agID);
		return agent;
	}
	
	
	/**
	 * Insert events after agent has been created.
	 * 
	 * @param agent
	 */
	private void createInitialAgentEvents(Agent agent) {
		TimeStamp ts = agent.getArrivalTime();
		
		eventManager.addActivity(new AgentArrival(agent, ts));
		
		// set liquidation at the end of the simulation for market makers
		if (agent instanceof BasicMarketMaker) {
			eventManager.addActivity(new Liquidate(agent, 
					agent.getModel().getFundamentalAt(data.simLength), data.simLength));
		}
	}
	

	/**
	 * Given a number of agents and a number of market IDs, splits them as evenly as 
	 * possible. Returns an array of length numAgents, each element being one of the 
	 * valid marketIDs. The distribution is as even as possible.
	 *
	 * @param numAgents
	 * @param mktIDs
	 */
	private ArrayList<Integer> assignAgToMkts(int numAgents, ArrayList<Integer> mktIDs) {
		ArrayList<Integer> assignment = new ArrayList<Integer>();
		
		if (numAgents == 0) return assignment;
		
		int remain = numAgents % mktIDs.size();
		int num = (numAgents - remain) / mktIDs.size();
		for (int i = 0; i < num; i++) {
			for (Integer id : mktIDs) {
				assignment.add(id);
			}
		}
		for (int i = 0; i < remain; i++) {
			assignment.add(mktIDs.get(i));
		}
		return assignment;
	}

	
	/**
	 * Logs agent information.
	 */
	public void logAgentInfo() {
		ArrayList<Integer> ids = data.getAgentIDs();
		for (Integer id : ids) {
			Agent ag = data.getAgent(id);
			
			// print arrival times
			String s = " " + ag.getID() + ": " + ag.toString() + "::" + 
					ag.getType() + "::";
			s += Agent.ARRIVAL_KEY + "=" + ag.getArrivalTime().toString();
			
			// print private value if exists 
			if (ag.hasPrivateValue()) {
				s += ", pv=" + ag.getPrivateValue();
			}
			s += " ... params=" + ag.getProperties().toString();
			Logger.log(Logger.INFO, s);
		}
	}

}
