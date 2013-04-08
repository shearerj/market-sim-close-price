package systemmanager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import activity.*;
import entity.*;
import event.*;
import model.*;

/**
 * SYSTEMSETUP
 * 
 * Class to create agent, models, and markets.
 * 
 * There are two use cases: EGTA analysis and Parallel simluation.
 * 	I) EGTA analysis
 * 		- In this use case, only ONE market model will be created. This is specified
 * 		  by the primary market model in the spec file.
 * 		- Model-level agents are not created. Both environment agents & players will
 * 		  be created.
 * 	II) Parallel simulation
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
 *   - create the primary model
 * - create the agents
 * 	 - environment agents, model-level agents, or players 
 *   - duplicate agents for each model (if parallel simulations)
 *   - agent-specific parameters are added to ObjectProperties
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
	
	private HashMap<AgentPropertiesPair, ArrayList<Integer>> logIDs;
	private HashMap<AgentPropertiesPair, ArrayList<Long>> seeds;
	private HashMap<Integer, MarketModel> modelMap;
	private HashMap<AgentPropertiesPair, Integer> allAgents;
	private HashMap<AgentPropertiesPair, ArrivalTime> arrivals;
	
	private HashMap<String, Integer> playerNumbers;
	private HashMap<String, ArrivalTime> playerArrivals;
	private HashMap<String, ArrayList<ObjectProperties>> playerStrategies;
	
	/**
	 * Constructor
	 * @param s
	 * @param em
	 * @param d
	 * @param l
	 */
	public SystemSetup(SimulationSpec s, EventManager em, SystemData d, Log l) {
		specs = s;
		eventManager = em;
		data = d;
		log = l;
		agentIDSequence = new Sequence(1);
		marketIDSequence = new Sequence(-1);
		modelIDSequence = new Sequence(1);
		
		logIDs = new HashMap<AgentPropertiesPair, ArrayList<Integer>>();
		seeds = new HashMap<AgentPropertiesPair, ArrayList<Long>>();
		modelMap = new HashMap<Integer, MarketModel>();
		allAgents = new HashMap<AgentPropertiesPair, Integer>();
		arrivals = new HashMap<AgentPropertiesPair, ArrivalTime>();
		playerNumbers = new HashMap<String, Integer>();
		playerArrivals = new HashMap<String, ArrivalTime>();
		playerStrategies = new HashMap<String, ArrayList<ObjectProperties>>();
	}
	
	
	public void setupAll() {
		try {
			// Generate private values
			data.globalFundamentalValues();
			data.EGTA = (data.getNumPlayers() > 0);
			
			// Must create market models before agents, so can add the agents
			// then to the appropriate/corresponding markets.
			createSIP();
			log.log(Log.INFO, "------------------------------------------------");
			log.log(Log.INFO, "            Creating MARKET MODELS");
			createMarketModels();
			log.log(Log.INFO, "------------------------------------------------");
			log.log(Log.INFO, "            Creating AGENTS");
			createAllAgents();

			// Log agent information
			logAgentInfo();
			log.log(Log.INFO, "------------------------------------------------");
			log.log(Log.INFO, " ");

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
	 * Create SIP entity, which enters the system at time 0.
	 */
	public void createSIP() {
		SIP iu = new SIP(0, data, log);
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
			String primaryModelType = desc[0];
			try {
				// always use default configuration of this market model
				MarketModel primary = createModel(primaryModelType, "");
				data.primaryModel = primary;
				log.log(Log.INFO, "Primary model: " + primaryModelType + "-(default)");
				
			} catch (Exception e) {
				if (!Arrays.asList(Consts.MARKETMODEL_TYPES).contains(primaryModelType)) {
					System.err.println(this.getClass().getSimpleName() + "::createMarketModels: " +
										"invalid primary market model type");
				}
				System.err.println(e.toString());
			}
			
		} else {
			// No players means parallel model simulation; no primary model (set as null)
			data.primaryModel = null;
			
			// Create by model type
			for (Map.Entry<String, Integer> mdl : data.numModelType.entrySet()) {
				
				int numModelsOfThisType = mdl.getValue();
				String modelType = mdl.getKey();
				log.log(Log.INFO, "Models: " + numModelsOfThisType + " " + modelType);
				
				// Parse the comma separated types of agents (list of configuration strings)
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
	private MarketModel createModel(String modelType, String configuration) {
		ObjectProperties p = new ObjectProperties(Consts.getProperties(modelType));
		
		// create market model & determine its configuration
		int modelID = modelIDSequence.increment();
		p.put(Consts.MODEL_CONFIG_KEY, configuration);
		MarketModel model = ModelFactory.createModel(modelType, modelID, p, data);		
		data.addModel(model);
		log.log(Log.INFO, model.getFullName() + ": " + model + " " + p);
		createMarketsInModel(model);
		return model;
	}
	
	
	/**
	 * Create markets in the given market model.
	 * @param model
	 */
	private void createMarketsInModel(MarketModel model) {
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
			
			data.marketIDModelIDMap.put(mID, model.getID());
		}
	}
	
	
	/**
	 * Creates the agents for each model. Each model has the same number of each 
	 * agent type (which is specified in the simulation spec file).
	 */
	private void createAllAgents() {
		
		if (data.isEGTAUseCase()) {
			/*******************
			 * EGTA - create agents only for primary model
			 *******************/
			// No model-level agents created in this use case
			modelMap.put(data.getPrimaryModel().getID(), data.getPrimaryModel());
			
		} else {
			/*******************
			 * PARALLEL SIMULATION - create environment & model agents
			 *******************/
			// Environment agents are present in EVERY market model
			modelMap.putAll(data.getModels());
			allAgents.putAll(data.getModelAgentNumberMap());
		}
		
		// add environment agents & set up their arrivals
		allAgents.putAll(data.getEnvAgentNumberMap());
		for (AgentPropertiesPair app : allAgents.keySet()) {
			// check if arrival rate already described for this AgentPropertiesPair
			double rate = data.arrivalRate;
			if (app.getProperties().containsKey(Agent.ARRIVALRATE_KEY)) {
				rate = Double.parseDouble(app.getProperties().get(Agent.ARRIVALRATE_KEY));
			}
			arrivals.put(app, new ArrivalTime(new TimeStamp(0), rate));
		}
		
		// set up their players & their arrivals
		for (Map.Entry<AgentPropertiesPair, Integer> entry : data.getPlayerNumberMap().entrySet()) {
			String type = entry.getKey().getAgentType();
			ObjectProperties o = entry.getKey().getProperties();
			int n = entry.getValue();
			
			if (playerNumbers.containsKey(type)) {
				playerNumbers.put(type, playerNumbers.get(type) + n);
				playerStrategies.get(type).addAll(Collections.nCopies(n, o));
				
			} else {
				playerNumbers.put(type, entry.getValue());
				playerArrivals.put(type, new ArrivalTime(new TimeStamp(0), data.arrivalRate));
				playerStrategies.put(type, new ArrayList<ObjectProperties>(Collections.nCopies(n, o)));
			}
		}
		
		// TODO pull out arrivals for future reference
		
		// generate logIDs and pseudorandom number generator seeds for non-players
		Random rand = new Random();
		int id = 1;
		for (Map.Entry<AgentPropertiesPair, Integer> entry : allAgents.entrySet()) {
			ArrayList<Integer> ids = new ArrayList<Integer>();
			ArrayList<Long> sd = new ArrayList<Long>();
			for (int i = 0; i < entry.getValue(); i++) {
				ids.add(id++);
				sd.add(rand.nextLong());
			}
			logIDs.put(entry.getKey(), ids);
			seeds.put(entry.getKey(), sd);
		}
		
		// create all agents
		for (MarketModel model : modelMap.values()) {
			log.log(Log.INFO, "MODEL: " + model.getFullName() + " agent types:");
			createNonPlayersForModel(model);
			if (data.isEGTAUseCase()) createPlayersForModel(model);
		}
		
	}

	/**
	 * Create players for the specified model.
	 * @param model
	 */
	private void createPlayersForModel(MarketModel model) {
		// only creates players in primary model
		try {
			for (Map.Entry<String, Integer> entry : playerNumbers.entrySet()) {
				String type = entry.getKey();
				int numAgents = entry.getValue();
				ArrayList<Integer> assignMktIDs = assignAgToMkts(numAgents, model.getMarketIDs());
				
				Random rand = new Random();
				for (int i = 0; i < numAgents; i++) {	
					// create copy of ObjectProperties in case modify it
					ObjectProperties op = new ObjectProperties(playerStrategies.get(type).get(i));
					if (data.isSMAgent(entry.getKey())) {
						// must assign market if single-market agent
						op.put(SMAgent.MARKETID_KEY, assignMktIDs.get(i).toString());
					}
					Agent ag = createAgent(model, new AgentPropertiesPair(type, op),
											rand.nextLong(), playerArrivals.get(type).next());
					ag.setLogID(ag.getID()); 	// set player ID = log ID
					data.addPlayer(ag);			// add player to SystemData
				}
				log.log(Log.INFO, "Players: " + numAgents + " " + type);
			}
		} catch (Exception e) {
			System.err.println(this.getClass().getSimpleName() + "::createPlayersForModel: error");
			e.printStackTrace();
		}
	}
	
	/**
	 * Creates environment agents in the specified model.
	 * @param model
	 */
	private void createNonPlayersForModel(MarketModel model) {
		try {
			for (Map.Entry<AgentPropertiesPair,Integer> a : allAgents.entrySet()) {
				AgentPropertiesPair ap = a.getKey();
				int numAgents = a.getValue();
				ArrayList<Integer> assignMktIDs = assignAgToMkts(numAgents, model.getMarketIDs());

				for (int i = 0; i < numAgents; i++) {			
					// create copy of ObjectProperties in case modify it
					ObjectProperties op = new ObjectProperties(ap.getProperties());
					if (data.isSMAgent(ap.getAgentType())) {
						// must assign market if single-market agent
						op.put(SMAgent.MARKETID_KEY, assignMktIDs.get(i).toString());
					}
					Agent ag = createAgent(model, new AgentPropertiesPair(ap.getAgentType(), op),
							seeds.get(ap).get(i), arrivals.get(ap).next());
					ag.setLogID(logIDs.get(ap).get(i));
				}
				log.log(Log.INFO, "Agents: " + numAgents + " " + ap.getAgentType() + " " +
						ap.getProperties());
			}
		} catch (Exception e) {
			System.err.println(this.getClass().getSimpleName() + "::createNonPlayersForModel: error");
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
	private Agent createAgent(MarketModel model, AgentPropertiesPair ap, Long seed, TimeStamp arr) { 
		String agType = ap.getAgentType();
		ObjectProperties p = ap.getProperties();
		
		p.put(Agent.RANDSEED_KEY, seed.toString());
		int agID = agentIDSequence.increment();
		if (data.isSMAgent(agType)) {
			// must assign market if single-market agent
			p.put(Agent.ARRIVAL_KEY, arr.toString());
			p.put(Agent.FUNDAMENTAL_KEY, data.getFundamentalAt(arr).toString());				
		}
		Agent agent = AgentFactory.createAgent(agType, agID, model.getID(), data, p, log);
		
		createInitialAgentEvents(agent);
		data.addAgent(agent);
		model.linkAgent(agID);
		log.log(Log.DEBUG, agent.toString() + ": " + p);
		return agent;
	}
	
	
	/**
	 * Insert events after agent has been created.
	 * 
	 * @param agent
	 */
	private void createInitialAgentEvents(Agent agent) {
		TimeStamp ts = agent.getArrivalTime();
		
		if (agent instanceof HFTAgent) {
			eventManager.createEvent(new AgentArrival(agent, ts));
//			eventManager.createEvent(new AgentDeparture(agent, data.simLength));
		}
		
		if (agent instanceof SMAgent) {
			Market mkt = ((SMAgent) agent).getMarket();
			eventManager.createEvent(Consts.SM_AGENT_PRIORITY, new AgentArrival(agent, mkt, ts));
//			eventManager.createEvent(new AgentDeparture(agent, mkt, data.simLength));
		}
		
		// set liquidation at the end of the simulation for market makers
		if (agent instanceof BasicMarketMaker) {
			eventManager.createEvent(Consts.LOWEST_PRIORITY, 
					new Liquidate(agent, data.getFundamentalAt(data.simLength), 
					data.simLength));
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
	private ArrayList<Integer> assignAgToMkts(int numAgents,
													 ArrayList<Integer> mktIDs) {
		ArrayList<Integer> assignment = new ArrayList<Integer>();
		
		if (numAgents == 0) return assignment;
		
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
	 * 2Logs agent information.
	 */
	public void logAgentInfo() {
		ArrayList<Integer> ids = data.getAgentIDs();
		for (Iterator<Integer> it = ids.iterator(); it.hasNext(); ) {
			Agent ag = data.getAgent(it.next());
			
			// print arrival times
			String s = " " + ag.getID() + ": " + ag.toString() + "::" + ag.getType() + "::";
			s += Agent.ARRIVAL_KEY + "=" + ag.getArrivalTime().toString();
			
			// print private value if exists 
			if (ag.getPrivateValue() >= 0) {
				s += ", pv=" + ag.getPrivateValue();
			}
			s += "...params=" + ag.getProperties().toString();
			log.log(Log.INFO, s);
		}
	}

}
