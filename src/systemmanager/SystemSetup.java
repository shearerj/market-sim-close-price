package systemmanager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Iterator;

import activity.*;
import entity.*;
import event.*;
import model.*;

/**
 * Class to create agents & markets.
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
 *   - setup the agents (different if MM vs SM)
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
			System.err.println(this.getClass().getSimpleName() + "::setupAll: error");
			System.err.println(e);
		}
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
				p.put(Consts.MODEL_CONFIG_KEY, configs[i]);
				MarketModel model = ModelFactory.createModel(modelType, p, data);		
				data.addModel(model);
				log.log(Log.INFO, model.getFullName() + ": ");
				
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
	 * Creates the agents.
	 */
	private void createAgents() {
		for (Map.Entry<String, Integer> ag : data.numAgentType.entrySet()) {
			for (int i = 0; i < ag.getValue(); i++) {
				ObjectProperties ap = getEntityProperties(ag.getKey(), i);
				
				// create agent & events
				String agentType = ag.getKey();
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
					
						// link the SM agent to all the other market models (other than the primary one)
						for (Map.Entry<Integer,MarketModel> entry : data.getModels().entrySet()) {
							if (!entry.getKey().equals(data.getPrimaryModel().getID())) {
								// if not the primary market model, link the agent
								entry.getValue().linkAgent(agentID);
							}
						}
						
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
			log.log(Log.INFO, "Agents: " + ag.getValue() + " " + ag.getKey());
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
