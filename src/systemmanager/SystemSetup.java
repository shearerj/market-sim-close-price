package systemmanager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import activity.*;
import entity.*;
import event.*;
import market.Price;
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
/**
 * @author ewah
 *
 */
public class SystemSetup {

	private Log log;
	private SystemData data;
	private EventManager eventManager;
	private SimulationSpec specs;
	
	private Sequence agentIDSequence;
	private Sequence marketIDSequence;
	private Sequence modelIDSequence;
	
	// Agent creation containers
	private ArrayList<Integer> logIDs;
	private ArrayList<Long> seeds;
	private ArrayList<TimeStamp> arrivals;
	private ArrayList<Price> fundamentalValues;
	
	
	public SystemSetup(SimulationSpec s, EventManager em, SystemData d, Log l) {
		specs = s;
		eventManager = em;
		data = d;
		log = l;
		agentIDSequence = new Sequence(1);
		marketIDSequence = new Sequence(-1);
		modelIDSequence = new Sequence(1);
		
		logIDs = new ArrayList<Integer>();
		seeds = new ArrayList<Long>();
		arrivals = new ArrayList<TimeStamp>();
		fundamentalValues = new ArrayList<Price>();
	}
	
	
	public void setupAll() {
		try {
			// Generate arrival times & private values
			data.arrivalTimes();
			data.globalFundamentalValues();

			// Must create market models before agents, so can add the agents
			// then to the appropriate/corresponding markets.
			createSIP();
			log.log(Log.INFO, "------------------------------------------------");
			log.log(Log.INFO, "    Creating MARKET MODELS");
			createMarketModels();
			log.log(Log.INFO, "------------------------------------------------");
			log.log(Log.INFO, "    Creating AGENTS");
			createAgents();

			// Log agent information
			logAgentInfo();
			log.log(Log.INFO, "    SETUP COMPLETE");
			log.log(Log.INFO, "------------------------------------------------");

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
		
		// Only create multiple market models if not an EGTA analysis (i.e. zero players)
		if (data.getNumPlayers() != 0) {
			
			// get information on the primary market model: (format MODELNAME-CONFIG)
			String[] desc = data.primaryModelDesc.split("[-]+");
			String primaryModelType;
			String primaryModelConfig;
			if (desc.length > 1) {
				// hard-coded to only take in first two words separated by hyphen
				primaryModelType = desc[0];
				primaryModelConfig = desc[1];
				
				// Create single primary model
				MarketModel primary = createModel(primaryModelType, primaryModelConfig);
				data.primaryModel = primary;
				log.log(Log.INFO, "Primary model: " + primaryModelType + "-" + primaryModelConfig);
				
			} else {
				System.err.println(this.getClass().getSimpleName() + "::createMarketModels: " +
									"primary market model config not specified");
				System.exit(1);
			}
			
		} else {
			// Parallel model simulation; no primary model (will be null)
			data.primaryModel = null;
			
			// Create by model type
			for (Map.Entry<String, Integer> mdl : data.numModelType.entrySet()) {
				
				int numModelsOfThisType = mdl.getValue();
				String modelType = mdl.getKey();
				log.log(Log.INFO, "Models: " + numModelsOfThisType + " " + modelType);
				
				// Parse the comma separated types of agents (list of configs)
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
	private void createAgents() {
		
		// Set initialization logIDs & seeds for all agents (environment + players)
		Random rand = new Random();
		int id = 1;	// start logID at 1
		for (int i = 0; i < data.getNumEnvAgents() + data.getNumPlayers(); i++) {
			TimeStamp t = data.nextArrival();
			arrivals.add(t);
			fundamentalValues.add(data.getFundamentalAt(t));
			logIDs.add(id++);
			seeds.add(rand.nextLong());
		}
		
		// TODO - model agents?
		
		
		
		

		if (data.getNumPlayers() > 0) {
			/*******************
			 * EGTA ONLINE - create agents only for primary model
			 *******************/
			
			log.log(Log.INFO, "MODEL: " + data.getPrimaryModel().getFullName() + 
						" agent types:");
			createEnvironmentAgents(data.getPrimaryModel());
			createPlayerAgents();
			
			
		} else {
			/*******************
			 * PARALLEL MODELS - create agents for all models
			 *******************/
			
			for (Map.Entry<Integer,MarketModel> entry : data.getModels().entrySet()) {
				MarketModel model = entry.getValue();
				log.log(Log.INFO, "MODEL: " + model.getFullName() + " agent types:");
				
				// Environment agents are present in EVERY market model
				createEnvironmentAgents(model);
				// DO NOT create players
			}	
		}
	}
	
	
	private void createEnvironmentAgents(MarketModel model) {
		
		int i = 0;
		for (Iterator<AgentPropertiesPair> it = data.getEnvAgentList().iterator(); it.hasNext(); ) {
			AgentPropertiesPair a = it.next();
			ArrayList<Integer> assignmt = assignAgentsToMarkets(data.envAgentNumberMap.get(a), model.getMarketIDs());
			// TODO - not the right place for this! 
			createAgentInModel(model, it.next(), i++);
			i++;
		}
		
//		for (Map.Entry<AgentPropertiesPair,Integer> ag : data.getEnvAgentList().entrySet()) {			
//			createAgentInModel(model, ag.getKey(), ag.getValue(), i++);
//		}
	}

	private void createPlayerAgents() {
		int i = data.getNumEnvAgents();
		for (Iterator<AgentPropertiesPair> it = data.getPlayerList().iterator(); it.hasNext(); ) {
			createAgentInModel(data.getPrimaryModel(), it.next(), i++);
		}
	}
	
	
	/**
	 * Create an agent with the specified properties & type in the given market model.
	 * @param model
	 * @param ap
	 * @param numAgents
	 * @param index
	 */
	private void createAgentInModel(MarketModel model, AgentPropertiesPair ap, int idx) { 
			//Long seed, int logID, TimeStamp arrival, Price value, int assignedMkt) {	
		
		String agentType = ap.getAgentType();
		ObjectProperties p = ap.getProperties();
		
		p.put("seed", seeds.get(idx).toString());
		int agentID = agentIDSequence.increment();
		Agent agent;
		
		if (!data.isSMAgent(agentType)) {
			// Multi-market agent
			agent = AgentFactory.createHFTAgent(agentType, agentID, model.getID(), data, p, log);
		} else {
			// Single market agent
			p.put("arrivalTime", arrivals.get(idx).toString());
			p.put("fundamental", fundamentalValues.get(idx).toString());
			int mktID = assignedMkt;
			
			agent = AgentFactory.createSMAgent(agentType, agentID, model.getID(), data, p, log, mktID);						
		}
		createInitialAgentEvents(agent);
		
		data.addAgent(agent);
		agent.setLogID(logIDs);
		model.linkAgent(agentID);
		log.log(Log.DEBUG, agent.toString() + ": " + p);
//		log.log(Log.INFO, "Agents: " + numAgents + " " + agentType);
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
	private ArrayList<Integer> assignAgentsToMarkets(int numAgents,
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
	 * Logs agent information (only for primary model to avoid redundancy).
	 */
	public void logAgentInfo() {
		ArrayList<Integer> ids = data.getAgentIDs();
		for (Iterator<Integer> it = ids.iterator(); it.hasNext(); ) {
			Agent ag = data.getAgent(it.next());
			
			// print arrival times
			String s = " " + ag.getID() + ": " + ag.toString() + "::" + ag.getType() + "::";
			s += "arrivalTime=" + ag.getArrivalTime().toString();
			
			// print private value if exists 
			if (ag.getPrivateValue() >= 0) {
				s += ", pv=" + ag.getPrivateValue();
			}
			log.log(Log.INFO, s);
		}
	}

}
