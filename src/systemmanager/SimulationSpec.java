package systemmanager;

import static systemmanager.Consts.AgentType.LA;
import static systemmanager.Consts.MarketType.CALL;
import static systemmanager.Consts.MarketType.CDA;
import static systemmanager.Keys.ARRIVAL_RATE;
import static systemmanager.Keys.CLEAR_FREQ;
import static systemmanager.Keys.FUNDAMENTAL_KAPPA;
import static systemmanager.Keys.FUNDAMENTAL_MEAN;
import static systemmanager.Keys.FUNDAMENTAL_SHOCK_VAR;
import static systemmanager.Keys.MARKET_LATENCY;
import static systemmanager.Keys.NBBO_LATENCY;
import static systemmanager.Keys.NUM;
import static systemmanager.Keys.NUM_SIMULATIONS;
import static systemmanager.Keys.PRIVATE_VALUE_VAR;
import static systemmanager.Keys.RAND_SEED;
import static systemmanager.Keys.REENTRY_RATE;
import static systemmanager.Keys.SIMULATION_LENGTH;
import static systemmanager.Keys.TICK_SIZE;

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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
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
			RAND_SEED, NBBO_LATENCY, NUM_SIMULATIONS };
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
		this.simulationProperties = EntityProperties.empty();
		this.defaultMarketProperties = EntityProperties.empty();
		this.defaultAgentProperties = EntityProperties.empty();

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
		
		playerProps = players == null ? new JsonObject() : players;
		simulationProperties = readProperties(config, simulationKeys);
	}

	public SimulationSpec(String specFileName) throws JsonSyntaxException,
			JsonIOException, FileNotFoundException {
		this(new File(specFileName));
	}

	protected static EntityProperties readProperties(JsonObject config,
			String... keys) {
		EntityProperties props = EntityProperties.empty();
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
				markets.add(MarketProperties.create(marketType, def, marketConfig));
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
				backgroundAgents.add(AgentProperties.create(agentType, def, agentConfig));
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
		if (preset.getAsString().isEmpty()) return;
		switch(Presets.valueOf(preset.getAsString())) {
		case NONE:
			break;
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
