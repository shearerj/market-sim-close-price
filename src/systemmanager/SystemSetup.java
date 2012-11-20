package systemmanager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Iterator;

import activity.*;
import entity.*;
import event.*;
import models.*;

/**
 * Class to create agents & markets. Sets up and assigns strategies (assumed to follow
 * format of parameter-value pairs separated by underscores: [param]_[value]_...
 * 
 * NOTE: Usage of the "primary_model" setting in the spec file:
 *  1) MODELNAME-TYPE
 *  2) MODELNAME (will first type in that model's list to determine primary model type)
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
	
	public SystemSetup(SimulationSpec s, EventManager em, SystemData d, Log l) {
		specs = s;
		eventManager = em;
		data = d;
		log = l;
		agentIDSequence = new Sequence(1);
		marketIDSequence = new Sequence(-1);
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
		} catch (Exception e) {
			System.err.print(e);
		}
	}
	
	
	public int nextMarketID() {
		return marketIDSequence.decrement();
	}
	
	/**
	 * Create Quoter entity, which enters the system at time 0.
	 */
	public void createQuoter() {
		Quoter iu = new Quoter(0, data, log);
		data.quoter = iu;
		eventManager.createEvent(new UpdateNBBO(iu, new TimeStamp(0)));
	}
	
	
	public void createMarketModels() {
		
		// get information on the primary market model - two possible usages:
		// 1) MODELNAME-TYPE
		// 2) MODELNAME (use first type in that model's list to determine type for primary)
		String[] desc = data.primaryModelDesc.split("[-]+");
		String primaryModel = data.primaryModelDesc;
		String primaryModelType;
		if (desc.length > 1) {
			// hard-coded to only take in first two words separated by hyphen, ignore rest
			primaryModel = desc[0];
			primaryModelType = desc[1];
		} else {
			// second usage; use first type found in the list
			primaryModelType = specs.getValue(primaryModel);
		}
		
		for (Map.Entry<String, Integer> mdl : data.numModelType.entrySet()) {
			
			// log before the markets are created
			log.log(Log.INFO, "Models: " + mdl.getValue() + " " + mdl.getKey());
			
			// Parse the comma separated types of agents
			String[] types = parseModelTypes(specs.getValue(mdl.getKey()));
			
			for (int i = 0; i < mdl.getValue(); i++) {
				ObjectProperties p = getEntityProperties(mdl.getKey(), i);
				
				// create market model & set its type
				p.put(Consts.MODEL_TYPE_KEY, types[i]);
				MarketModel model = ModelFactory.createModel(mdl.getKey(), p, data);		
				data.addModel(model);
				log.log(Log.DEBUG, model + ": " + p);
				// create markets in the model
				model.createMarkets(this, data);
				
				// set up primary model
				if (mdl.getKey().equals(primaryModel)) {
					// check if primary model type is contained in the specs file
					if (Arrays.asList(types).contains(primaryModelType)) {
						// check that the current model type matches
						if (types[i].equals(primaryModelType)) {
							if (data.primaryModel == null) {
								// only initialize primary model once
								data.primaryModel = model;
							}
						}
					} else {
						System.err.println(this.getClass().getSimpleName() + ":" + 
								"createMarketModels: " + "primary model type not found in " +
								"types list for " + mdl.getKey());
					}
				}
			}
		}
		
		// log primary model
		log.log(Log.INFO, "Primary model: " + primaryModel + "-" + primaryModelType);
		
		// set the model to market list
		for (Map.Entry<Integer,MarketModel> mdl : data.models.entrySet()) {
			data.modelToMarketList.put(mdl.getKey(), mdl.getValue().getMarketIDs());
		}
	}
	
	/**
	 * Parse list of model types from simulation specifications file.
	 * 
	 * @param modelTypeList
	 * @return
	 */
	private String[] parseModelTypes(String modelTypeList) {
		String[] types = null;
		if (modelTypeList != null) {
			if (modelTypeList.endsWith(",")) {
				// remove any extra appended commas
				modelTypeList = modelTypeList.substring(0, modelTypeList.length() - 1);
			}
			types = modelTypeList.split("[,]+");
		}
		return types;
	}
	
	/**
	 * Creates market and initializes any Activities as necessary. For example,
	 * for Call Markets, this method inserts the initial Clear activity into the 
	 * eventQueue.
	 * 
	 * @param marketID
	 * @param marketType
	 * @param mp EntityProperties object
	 * @param modelID
	 */
	public void setupMarket(int marketID, String marketType, ObjectProperties mp, int modelID) {
		Market market = MarketFactory.createMarket(marketType, marketID, data, mp, log);
		market.linkModel(modelID);
		data.addMarket(market);
		log.log(Log.DEBUG, market.toString() + ": " + mp);
		
		// Check if is call market, then initialize clearing sequence
		if (market instanceof CallMarket) {
			Activity clear = new Clear(market, market.getNextClearTime());
			eventManager.createEvent(Consts.CALL_CLEAR_PRIORITY, clear);
		}
		log.log(Log.INFO, "Markets: " + market.getType() + ", " + market);
	}
	
	
	
	/**
	 * Creates the agents.
	 */
	private void createAgents() {
		for (Map.Entry<String, Integer> ag : data.numAgentType.entrySet()) {
			for (int i = 0; i < ag.getValue(); i++) {
				ObjectProperties ap = getEntityProperties(ag.getKey(), i);
				
				// create agent & events
				setupAgent(ag.getKey(), ap);
			}
			log.log(Log.INFO, "Agents: " + ag.getValue() + " " + ag.getKey());
		}
	}
	

	/**
	 * Creates agent and initializes all agent settings/parameters.
	 * Inserts AgentArrival/Departure activities into the eventQueue.
	 * 
	 * @param agentType
	 * @param ap EntityProperties object
	 */
	public void setupAgent(String agentType, ObjectProperties ap) {
		
		if (!Arrays.asList(Consts.SMAgentTypes).contains(agentType)) {
			// Multi-market agent
			int agentID = agentIDSequence.increment();
			Agent agent = AgentFactory.createMMAgent(agentType, agentID, data, ap, log);
			data.addAgent(agent);
			log.log(Log.DEBUG, agent.toString() + ": " + ap);
			
			TimeStamp ts = agent.getArrivalTime();
			if (agent instanceof MMAgent) {
				// Agent is in multiple markets (an extra check)
				eventManager.createEvent(new AgentArrival(agent, ts));
				eventManager.createEvent(new AgentDeparture(agent, data.simLength));
			}
			// check if in a role, keep track of role agent IDs
			if (Arrays.asList(Consts.roles).contains(agentType)) {
				data.roleAgentIDs.add(agentID);
			}
			
		} else {
			// Single market agent - create one for each market in primary model
			for (Iterator<Integer> it = data.getPrimaryMarketIDs().iterator(); it.hasNext(); ) {
				int mktID = it.next();
				int agentID = agentIDSequence.increment();
				Agent agent = AgentFactory.createSMAgent(agentType, agentID, data, ap, log, mktID);
				data.addAgent(agent);
				log.log(Log.DEBUG, agent.toString() + ": " + ap);
			
				// add the agent to all the other market models (other than the primary one)
				linkAgentToModels(agentID);
				
				TimeStamp ts = agent.getArrivalTime();
				if (agent instanceof SMAgent) {
					// Agent is in single market (an extra check)
					Market mkt = ((SMAgent) agent).getMarket();
					eventManager.createEvent(new AgentArrival(agent, mkt, ts));
					eventManager.createEvent(new AgentDeparture(agent, mkt, data.simLength));		
				}
				// check if in a role, keep track of role agent IDs
				if (Arrays.asList(Consts.roles).contains(agentType)) {
					data.roleAgentIDs.add(agentID);
				}
			}
		}
		
		
	}
	
	
	/**
	 * Link a given SM agent to all models other than the primary model.
	 * 
	 * @param agentID
	 */
	public void linkAgentToModels(int agentID) {
		for (Map.Entry<Integer,MarketModel> entry : data.getModels().entrySet()) {
			if (!entry.getKey().equals(data.getPrimaryModel().hashCode())) {
				// if not the primary market model, link the agent
				entry.getValue().linkAgent(agentID);
			}
		}
	}
	
	/**
	 * Logs agent information.
	 */
	public void logAgentInfo() {
		for (Map.Entry<Integer,Agent> entry : data.agents.entrySet()) {
			Agent ag = entry.getValue();
			
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
	 *
	 * @param type	Entity type
	 * @param idx	index of the role for which to set the strategy, -1 otherwise
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
