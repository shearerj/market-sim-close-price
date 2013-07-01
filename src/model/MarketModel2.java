package model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import market.Bid;
import market.Price;
import market.Transaction;
import utils.CollectionUtils;
import utils.RandPlus;
import data.AgentProperties;
import data.FundamentalValue;
import data.ObjectProperties;
import entity.Agent;
import entity.Market;
import entity.SMAgent;
import entity.SMAgentFactory;
import event.TimeStamp;
import generators.Generator;
import generators.PoissonArrivalGenerator;
import generators.RoundRobinGenerator;

/**
 * MARKETMODEL
 * 
 * Base class for specifying a market model (e.g. two-market model, centralized
 * call market, etc.).
 * 
 * Multiple types of market models can be included in a single simulation trial.
 * Agents present in the simulation will reside in a primary market model (for
 * payoff output purposes in the observation files), but agents behave
 * independently within each model.
 * 
 * If the market model has only one market, then it is assumed to be a
 * "centralized" model. Note that not only can market properties be set here,
 * but also the assignments of agents to markets. The number of agents of each
 * type is fixed globally, but all or a subset of these agents can be assigned
 * to be in any of the available markets (single-market agents). If not
 * specified, all background agents are assumed to trade in all available
 * markets in the model.
 * 
 * Note:
 * 
 * Configuration: - Each model may have various configurations, e.g. specifying
 * the players allowed in that instance of the model. - Each configuration is a
 * string, and they are separated by commas.
 * 
 * For example, in the spec file:
 * 
 * "MARKETMODEL": "A,B"
 * 
 * would indicate that for the given model, there is one instance of
 * configuration A and one instance of configuration B. The system, in this
 * case, would also determine that it needs to create two instances of this
 * model.
 * 
 * @author ewah
 */
public abstract class MarketModel2 {

	protected final RandPlus rand;
	protected final int modelID;
	protected final FundamentalValue fundamentalGenerator;
	protected final Collection<Market> markets;
	protected final Collection<Agent> environmentalAgents;
	protected final Collection<Agent> modelAgents;
	protected final Collection<Transaction> trans;

	protected int nextAgentID = 1;

	public MarketModel2(int modelID, FundamentalValue fundamental,
			Collection<Market> markets,
			Map<AgentProperties, Integer> agentProps,
			ObjectProperties modelProps, RandPlus rand) {
		
		this.modelID = modelID;
		this.rand = rand;
		this.fundamentalGenerator = fundamental;
		// XXX Perhaps HashSets instead of ArrayLists?
		this.markets = markets;
		this.environmentalAgents = new ArrayList<Agent>();
		this.modelAgents = new ArrayList<Agent>();
		this.trans = new ArrayList<Transaction>();

		for (Entry<AgentProperties, Integer> type : agentProps.entrySet()) {
			// FIXME Replace null with this for true MarketModel2 and 0 with
			// default arrival rate
			AgentProperties agProps = type.getKey();
			int number = type.getValue();
			Generator<TimeStamp> arrivals = new PoissonArrivalGenerator(
					TimeStamp.startTime,
					modelProps.getAsLong("arrival-rate", 0), new RandPlus(
							rand.nextLong()));
			Generator<Market> marketRate = new RoundRobinGenerator<Market>(
					markets);
			
			SMAgentFactory factory = new SMAgentFactory(null, agProps,
					nextAgentID, number, arrivals, marketRate, new RandPlus(
							rand.nextLong()));

			for (SMAgent agent: CollectionUtils.toIterable(factory)) {
				environmentalAgents.add(agent);
				// TODO schedule agent arrival
			}
			nextAgentID += number;
		}
	}

	/**
	 * @return number of (additional, non-environment) agents
	 */
	public int getNumModelAgents() {
		return modelAgents.size();
	}

	public Price getFundamentalAt(TimeStamp ts) {
		if (fundamentalGenerator == null)
			throw new IllegalStateException("No Fundamental Value...");
		return fundamentalGenerator.getValueAt(ts);
	}

	/**
	 * @return number of markets in the model
	 */
	public int getNumMarkets() {
		return markets.size();
	}

	public int getID() {
		return modelID;
	}

	public Collection<Transaction> getTrans() {
		return Collections.unmodifiableCollection(trans);
	}

	public void addTrans(Transaction tr) {
		this.trans.add(tr);
	}

	public ArrayList<Bid> getAllBids() {
		ArrayList<Bid> modelBids = new ArrayList<Bid>();
		for (Market mkt : markets) {
			modelBids.addAll(mkt.getAllBids());
		}
		return modelBids;
	}

	public Collection<Market> getMarkets() {
		return Collections.unmodifiableCollection(markets);
	}

	public SortedMap<Integer, TimeStamp> getExecutionTimes() {
		TreeMap<Integer, TimeStamp> executionTimes = new TreeMap<Integer, TimeStamp>();
		for (Market market : markets) {
			executionTimes.putAll(market.getExecutionTimes());
		}
		return executionTimes;
	}

	@Override
	public String toString() {
		return new String("{" + getID() + "}");
	}
}
