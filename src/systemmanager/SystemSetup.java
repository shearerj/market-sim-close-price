package systemmanager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import market.*;
import activity.*;
import entity.*;
import event.*;

/**
 * Class to create agents & markets. Sets up and assigns strategies (assumed to follow
 * format of parameter-value pairs separated by underscores: [param]_[value]_...
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
		
		// Generate arrival times & private values
		data.backgroundArrivalTimes();
		data.backgroundPrivateValues();
		
		// Create entities
		createQuoter();
		createMarkets();
		createAgents();
		
		// Log agent information
		logAgentInfo();
	}
	
	
	/**
	 * Create Quoter entity, which enters the system at time 0
	 */
	public void createQuoter() {
		Quoter iu = new Quoter(0, data, log);
		data.quoter = iu;
		eventManager.createEvent(new UpdateNBBO(iu, new TimeStamp(0)));
	}
	
	/**
	 * Creates market and initializes any Activities as necessary. For example,
	 * for Call Markets, this method inserts the initial Clear activity into the 
	 * eventQueue.
	 * 
	 * @param marketID
	 * @param marketType
	 * @param mp EntityProperties object
	 */
	public void setupMarket(int marketID, String marketType, EntityProperties mp) {
		
		Market market;
//		if (marketType.startsWith(Consts.CENTRAL)) {
//			market = MarketFactory.createMarket(marketType.substring(Consts.CENTRAL.length()+1),
//					marketID, data, mp, log);
//			data.centralMarkets.put(marketID, market);
//		} else {
			// Only add market to the general list if it's not the central market
			market = MarketFactory.createMarket(marketType, marketID, data, mp, log);
			data.addMarket(market);
			log.log(Log.DEBUG, market.toString() + ": " + mp);
//		}
		
		// Check if is call market, then initialize clearing sequence
		if (market instanceof CallMarket) {
			Activity clear = new Clear(market, market.getNextClearTime());
			eventManager.createEvent(Consts.CALL_CLEAR_PRIORITY, clear);
		}
	}
	

	/**
	 * Creates agent and initializes all agent settings/parameters.
	 * Inserts AgentArrival/Departure activities into the eventQueue.
	 * 
	 * @param agentID
	 * @param agentType
	 * @param ap EntityProperties object
	 */
	public void setupAgent(int agentID, String agentType, EntityProperties ap) {
		Agent agent = AgentFactory.createAgent(agentType, agentID, data, ap, log);
		data.addAgent(agent);
		log.log(Log.DEBUG, agent.toString() + ": " + ap);
		
		TimeStamp ts = agent.getArrivalTime();
		
		if (agent instanceof SMAgent) {
			// Agent is in single market
			for (int i = 1; i <= data.numMarkets; i++) {
				Market mkt = data.getMarket(-i);
				eventManager.createEvent(new AgentArrival(agent, mkt, ts));
				eventManager.createEvent(new AgentDeparture(agent, mkt, data.simLength));
			}
			
		} else if (agent instanceof MMAgent) {
			// Agent is in multiple markets
			eventManager.createEvent(new AgentArrival(agent, ts));
			eventManager.createEvent(new AgentDeparture(agent, data.simLength));
		}
	}
	
	
	
	public void createMarkets() {
		for (Map.Entry<String, Integer> mkt: data.numMarketType.entrySet()) {
			
//			if (mkt.getKey().startsWith(Consts.CENTRAL)) {
//				int mID = marketIDSequence.decrement();
//				setupMarket(mID, mkt.getKey());
//				log.log(Log.INFO, mkt.getKey() + " Market: " + data.getMarket(mID));
//				
//			} else {
				for (int i = 0; i < mkt.getValue(); i++) {
					EntityProperties mp = getEntityProperties(mkt.getKey(), i);
					int mID = marketIDSequence.decrement();
					// create market
					setupMarket(mID, mkt.getKey(), mp);	
				}
				log.log(Log.INFO, "Markets: " + mkt.getValue() + " " + mkt.getKey());
//			}
		}
	}
	
	public void createAgents() {
		for (Map.Entry<String, Integer> ag : data.numAgentType.entrySet()) {
			for (int i = 0; i < ag.getValue(); i++) {
				EntityProperties ap = getEntityProperties(ag.getKey(), i);
				int aID = agentIDSequence.increment();

				// create agent & events
				setupAgent(aID, ag.getKey(), ap);
				
				// check if in a role, keep track of role agent IDs
				if (Arrays.asList(Consts.roles).contains(ag.getKey())) {
					data.roleAgentIDs.add(aID);
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
	 * Sets params for an entity. May overwrite the default EntityProperties set in
	 * Consts. If the entity type indicates that the entity is a player in a role, 
	 * this method parses the strategy, if any, in the simulation spec file. 
	 *
	 * @param type	Entity type
	 * @param idx	index of the role for which to set the strategy, -1 otherwise
	 * @return EntityProperties
	 */
	public EntityProperties getEntityProperties(String type, int idx) {
		if (specs.getRoleStrategies().containsKey(type) && idx >= 0) {
			EntityProperties p = new EntityProperties(Consts.getProperties(type));
			
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
			return new EntityProperties(Consts.getProperties(type));
		}
	}

}
