package systemmanager;

import static com.google.common.base.Predicates.equalTo;
import static systemmanager.Consts.AgentType.LA;
import static systemmanager.Consts.MarketType.CALL;
import static systemmanager.Consts.MarketType.CDA;
import static systemmanager.Keys.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.io.Serializable;
import java.util.Collection;

import systemmanager.Consts.AgentType;
import systemmanager.Consts.MarketType;
import systemmanager.Consts.Presets;

import com.google.common.base.Splitter;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

import data.AgentProperties;
import data.EntityProperties;
import data.MarketProperties;

/**
 * Stores list of web parameters used in EGTAOnline.
 * 
 * NOTE: All MarketModel types in the spec file must match the corresponding
 * class name.
 * 
 * @author ewah
 */
public class SimulationSpec implements Serializable {

	private static final long serialVersionUID = 5646083286397841102L;
	protected static final Splitter split = Splitter.on(';');
	protected static final Gson gson = new Gson();
	
	protected static final String[] simulationKeys = { SIMULATION_LENGTH,
			FUNDAMENTAL_MEAN, FUNDAMENTAL_KAPPA, FUNDAMENTAL_SHOCK_VAR,
			RAND_SEED, NBBO_LATENCY, MODEL_NAME, MODEL_NUM, NUM_SIMULATIONS };
	protected static final String[] marketKeys = { MARKET_LATENCY, TICK_SIZE };
	protected static final String[] agentKeys = { TICK_SIZE, ARRIVAL_RATE,
			REENTRY_RATE, PRIVATE_VALUE_VAR };

	protected transient final JsonObject rawSpec;
	protected final EntityProperties simulationProperties;
	protected final EntityProperties defaultMarketProperties;
	protected final EntityProperties defaultAgentProperties;

	protected final Collection<MarketProperties> marketProps;
	protected final Collection<AgentProperties> agentProps;
	protected transient final JsonObject playerProps; // TODO Change to properties object

	public SimulationSpec() {
		this.rawSpec = new JsonObject();
		this.simulationProperties = new EntityProperties();
		this.defaultMarketProperties = new EntityProperties();
		this.defaultAgentProperties = new EntityProperties();

		this.marketProps = ImmutableList.of();
		this.agentProps = ImmutableList.of();
		this.playerProps = new JsonObject();
	}
	
	public SimulationSpec(File specFile) throws FileNotFoundException {
		this(new FileReader(specFile));
	}
	
	public SimulationSpec(Reader reader) {
		rawSpec = gson.fromJson(reader, JsonObject.class);
		JsonObject config = rawSpec.getAsJsonObject(Keys.CONFIG);
		JsonObject players = rawSpec.getAsJsonObject(Keys.ASSIGN);

		presets(config);
		
		defaultMarketProperties = readProperties(config, marketKeys);
		marketProps = markets(config, defaultMarketProperties);
		
		defaultAgentProperties = readProperties(config, agentKeys);
		agentProps = agents(config, defaultAgentProperties);
		
		getName(config, marketProps, agentProps);
		
		playerProps = players;
		simulationProperties = readProperties(config, simulationKeys);
	}

	public SimulationSpec(String specFileName) throws JsonSyntaxException,
			JsonIOException, FileNotFoundException {
		this(new File(specFileName));
	}

	protected static EntityProperties readProperties(JsonObject config,
			String... keys) {
		EntityProperties props = new EntityProperties();
		for (String key : keys) {
			JsonPrimitive value = config.getAsJsonPrimitive(key);
			if (value == null) continue;
			props.put(key, value.getAsString());
		}
		return props;
	}

	protected Collection<MarketProperties> markets(
			JsonObject config, EntityProperties def) {
		Builder<MarketProperties> markets = ImmutableList.builder();

		for (MarketType marketType : MarketType.values()) {
			JsonPrimitive configJson = config.getAsJsonPrimitive(marketType.toString());
			if (configJson == null) continue;
			for (String marketConfig : split.split(configJson.getAsString()))
				markets.add(new MarketProperties(marketType, def, marketConfig));
		}
		return markets.build();
	}

	protected Collection<AgentProperties> agents(JsonObject config,
			EntityProperties def) {
		Builder<AgentProperties> backgroundAgents = ImmutableList.builder();

		for (AgentType agentType : Consts.AgentType.values()) {
			JsonPrimitive configJson = config.getAsJsonPrimitive(agentType.toString());
			if (configJson == null) continue;
			for (String agentConfig : split.split(configJson.getAsString()))
				backgroundAgents.add(new AgentProperties(agentType, def, agentConfig));
		}
		return backgroundAgents.build();
	}
	
	/**
	 * Set preset for standard simulations
	 */
	// Just add a new case to add your own!
	protected void presets(JsonObject config) {
		JsonPrimitive preset = config.getAsJsonPrimitive(Keys.PRESETS);
		if (preset == null) return;
		switch(Presets.valueOf(preset.getAsString())) {
		case TWOMARKET:
			config.addProperty(CDA.toString(), NUM + "_2");
			config.addProperty(CALL.toString(), NUM + "_0");
			config.addProperty(LA.toString(), NUM + "_0");
			break;
		case TWOMARKETLA:
			config.addProperty(CDA.toString(), NUM + "_2");
			config.addProperty(CALL.toString(), NUM + "_0");
			config.addProperty(LA.toString(), NUM + "_1");
			break;
		case CENTRALCDA:
			config.addProperty(CDA.toString(), NUM + "_1");
			config.addProperty(CALL.toString(), NUM + "_0");
			config.addProperty(LA.toString(), NUM + "_0");
			break;
		case CENTRALCALL:
			int nbboLatency = config.getAsJsonPrimitive(NBBO_LATENCY).getAsInt();
			config.addProperty(CDA.toString(), NUM + "_0");
			config.addProperty(CALL.toString(), NUM + "_1_" + CLEAR_FREQ + "_" + nbboLatency);
			config.addProperty(LA.toString(), NUM + "_0");
			break;
		default:
			// Should be impossible to reach here
			throw new IllegalArgumentException("Unknown Preset");
		}
		
	}
	
	/**
	 * Used to generate a unique name for the simulation. If a name is given in the spec, it will
	 * use that name. Otherwise if there's a preset, it will use the name of the preset. If there's
	 * no preset it will form a string with the number of markets, LA agents, and the model number.
	 * The central call market would simply be called 1CALL_1. TWOMARKET with an LA would be
	 * 2CDA_1LA_1. A hypothetical model with 2 CDA markets, 1 call market, and 2 la agents would be
	 * 2CDA_1CALL_2LA_1.
	 */
	// XXX Move to System Manager? I'm not sure this should go here.
	protected void getName(JsonObject config,
			Collection<MarketProperties> marketProps,
			Collection<AgentProperties> agentProps) {
		if (!config.has(Keys.MODEL_NUM)) config.addProperty(Keys.MODEL_NUM, 1);
		if (config.has(Keys.MODEL_NAME)) return;
		JsonPrimitive preset = config.getAsJsonPrimitive(Keys.PRESETS);
		if (preset != null) {
			// Use preset
			config.addProperty(Keys.MODEL_NAME, preset.getAsString());
		} else {
			// Use config
			Multiset<MarketType> marketCounts = HashMultiset.create();
			for (MarketProperties props : marketProps)
				marketCounts.add(props.getMarketType(), props.getAsInt(Keys.NUM, 0));

			Multiset<AgentType> agentCounts = HashMultiset.create();
			for (AgentProperties props : agentProps)
				agentCounts.add(props.getAgentType(), props.getAsInt(Keys.NUM, 0));

			int num = config.getAsJsonPrimitive(Keys.MODEL_NUM).getAsInt();
			
			StringBuilder sb = new StringBuilder();
			for (Multiset.Entry<MarketType> e : marketCounts.entrySet())
				sb.append(e.getCount()).append(e.getElement()).append('_');
			Iterables.filter(agentCounts.elementSet(), equalTo(LA));
			for (Multiset.Entry<AgentType> e : agentCounts.entrySet())
				if (e.getElement() == LA)
					sb.append(e.getCount()).append(e.getElement()).append('_');
			sb.append(num);
			config.addProperty(Keys.MODEL_NAME, sb.toString());
		}
	}

	public EntityProperties getSimulationProps() {
		return simulationProperties;
	}
	
	public EntityProperties getDefaultMarketProps() {
		return defaultMarketProperties;
	}
	
	public EntityProperties getDefaultAgentProps() {
		return defaultAgentProperties;
	}

	public Collection<MarketProperties> getMarketProps() {
		return ImmutableList.copyOf(marketProps);
	}

	public Collection<AgentProperties> getAgentProps() {
		return ImmutableList.copyOf(agentProps);
	}

	public JsonObject getPlayerProps() {
		return playerProps;
	}
	
	public JsonObject getRawSpec() {
		return rawSpec;
	}
	
	@Override
	public String toString() {
		return rawSpec.toString();
	}

}
