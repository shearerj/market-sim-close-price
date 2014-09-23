package systemmanager;

import static systemmanager.Consts.AgentType.LA;
import static systemmanager.Consts.MarketType.CALL;
import static systemmanager.Consts.MarketType.CDA;
import static systemmanager.Keys.CLEAR_FREQ;
import static systemmanager.Keys.NBBO_LATENCY;
import static systemmanager.Keys.NUM;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Map.Entry;

import systemmanager.Consts.AgentType;
import systemmanager.Consts.MarketType;
import systemmanager.Consts.Presets;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import data.Props;

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
	
	private static final Splitter propSplit = Splitter.on(';');
	private static final Splitter paramSplit = Splitter.on('_');
	private static final Splitter typeSplit = Splitter.on(':');

	private transient final JsonObject rawSpec;
	
	private final Props simulationProperties;
	private final Multimap<MarketType, Props> marketProps;
	private final Multimap<AgentType, Props> agentProps;
	private final Multiset<PlayerSpec> playerProps; 
	
	protected SimulationSpec() {
		this.rawSpec = new JsonObject();
		this.simulationProperties = Props.fromPairs();
		this.marketProps = ImmutableMultimap.of();
		this.agentProps = ImmutableMultimap.of();
		this.playerProps = ImmutableMultiset.of();
	}
	
	protected SimulationSpec(JsonObject rawSpec) {
		this.rawSpec = rawSpec;
		JsonObject config = Optional.fromNullable(rawSpec.getAsJsonObject(Keys.CONFIG)).or(new JsonObject());
		JsonObject players = Optional.fromNullable(rawSpec.getAsJsonObject(Keys.ASSIGN)).or(new JsonObject());

		parsePresets(config);
		
		this.simulationProperties = readProperties(config);
		this.marketProps = markets(config, simulationProperties);
		this.agentProps = agents(config, simulationProperties);
		this.playerProps = players(players, simulationProperties);
	}
	
	public static SimulationSpec empty() {
		return new SimulationSpec();
	}
	
	public static SimulationSpec fromJson(JsonObject json) {
		return new SimulationSpec(json);
	}

	protected static Props readProperties(JsonObject config) {
		ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
		for (Entry<String, JsonElement> e : config.entrySet())
			builder.put(e.getKey(), e.getValue().getAsString());
		return Props.fromMap(builder.build());
	}

	protected static Multimap<MarketType, Props> markets(JsonObject config, Props defaultProps) {
		ImmutableMultimap.Builder<MarketType, Props> markets = ImmutableMultimap.builder();
		
		for (MarketType marketType : MarketType.values())
			if (config.has(marketType.toString()))
				for (String marketConfig : propSplit.split(config.get(marketType.toString()).getAsString()))
					markets.put(marketType, Props.withDefaults(defaultProps, paramSplit.split(marketConfig)));
		
		return markets.build();
	}

	protected static Multimap<AgentType, Props> agents(JsonObject config, Props defaultProps) {
		ImmutableMultimap.Builder<AgentType, Props> agents = ImmutableMultimap.builder();

		for (AgentType agentType : Consts.AgentType.values())
			if (config.has(agentType.toString()))
				for (String agentConfig : propSplit.split(config.get(agentType.toString()).getAsString()))
					agents.put(agentType, Props.withDefaults(defaultProps, paramSplit.split(agentConfig)));
		
		return agents.build();
	}
	
	protected static Multiset<PlayerSpec> players(JsonObject config, Props defaults) {
		ImmutableMultiset.Builder<PlayerSpec> players = ImmutableMultiset.builder();
		
		for (Entry<String, JsonElement> e : config.entrySet()) {
			for (JsonElement stratString : e.getValue().getAsJsonArray()) {
				String strat = stratString.getAsString();
				Iterator<String> split = typeSplit.split(strat).iterator();
				players.add(new PlayerSpec(e.getKey(), strat, AgentType.valueOf(split.next()), Props.withDefaults(defaults,
						split.hasNext() ? paramSplit.split(split.next()) : ImmutableList.of())));
			}
		}
		return players.build();
	}
	
	/**
	 * Set preset for standard simulations
	 */
	// Just add a new case to add your own!
	protected static void parsePresets(JsonObject config) {
		JsonPrimitive preset = config.getAsJsonPrimitive(Keys.PRESETS);
		if (preset == null || preset.getAsString().isEmpty())
			return;
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
			throw new IllegalArgumentException("Unknown Preset");
		}
		
	}

	public Props getSimulationProps() {
		return simulationProperties;
	}

	public Multimap<MarketType, Props> getMarketProps() {
		return marketProps;
	}

	public Multimap<AgentType, Props> getAgentProps() {
		return agentProps;
	}

	public Multiset<PlayerSpec> getPlayerProps() {
		return playerProps;
	}
	
	public JsonObject getRawSpec() {
		return rawSpec;
	}
	
	@Override
	public String toString() {
		return rawSpec.toString();
	}
	
	// I don't love this class, but it works...
	public static class PlayerSpec {
		public final String descriptor;
		public final AgentType type;
		public final Props agentProps;
		
		public PlayerSpec(String role, String strategy, AgentType type, Props agentProps) {
			this.descriptor = role + '_' + strategy;
			this.type = type;
			this.agentProps = agentProps;
		}
	}

	public static class SimSpecSerializer implements JsonSerializer<SimulationSpec> {
		@Override
		public JsonElement serialize(SimulationSpec src, Type typeOfSrc, JsonSerializationContext context) {
			return src.rawSpec;
		}
	}

	public static class SimSpecDeserializer implements JsonDeserializer<SimulationSpec> {
		@Override
		public SimulationSpec deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			return new SimulationSpec(json.getAsJsonObject());
		}
	}

}
