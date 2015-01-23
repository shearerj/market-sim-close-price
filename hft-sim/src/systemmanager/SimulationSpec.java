package systemmanager;

import java.io.Reader;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import systemmanager.Consts.AgentType;
import systemmanager.Consts.MarketType;

import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import data.Preset;
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
	
	private static final Gson gsonReader = new GsonBuilder()
		.registerTypeAdapter(SimulationSpec.class, new SimSpecDeserializer())
		.create();

	public static final String CONFIG = "configuration";
	public static final String ASSIGNMENT = "assignment";
	
	private static final Splitter propSplit = Splitter.on(';');
	private static final Splitter paramSplit = Splitter.on('_').omitEmptyStrings();
	private static final Splitter typeSplit = Splitter.on(':');
	
	// These are simulation specification keys, but not valid class keys
	private static final Set<String> entityKeys = ImmutableSet.<String> builder()
			.addAll(Iterables.transform(Arrays.asList(MarketType.values()), Functions.toStringFunction()))
			.addAll(Iterables.transform(Arrays.asList(AgentType.values()), Functions.toStringFunction()))
			.build();
	
	private transient final JsonObject rawSpec;
	
	private final Props simulationProperties;
	private final Multimap<MarketType, Props> marketProps;
	private final Multimap<AgentType, Props> agentProps;
	private final Multiset<PlayerSpec> playerProps; 
	
	protected SimulationSpec() {
		this(new JsonObject(), Props.fromPairs(), ImmutableMultimap.<MarketType, Props> of(), ImmutableMultimap.<AgentType, Props> of(),
				ImmutableMultiset.<PlayerSpec> of());
	}
	
	protected SimulationSpec(JsonObject rawSpec, Props simulationProperties, Multimap<MarketType, Props> marketProps,
			Multimap<AgentType, Props> agentProps, Multiset<PlayerSpec> playerProps) {
		this.rawSpec = rawSpec;
		this.simulationProperties = simulationProperties;
		this.marketProps = marketProps;
		this.agentProps = agentProps;
		this.playerProps = playerProps;
	}
	
	public static SimulationSpec fromJson(JsonObject spec) {
		JsonObject config = Preset.parsePresets(Optional.fromNullable(spec.getAsJsonObject(CONFIG)).or(new JsonObject()));
		JsonObject players = Optional.fromNullable(spec.getAsJsonObject(ASSIGNMENT)).or(new JsonObject());
		
		Props simulationProperties = readProperties(config);
		Multimap<MarketType, Props> marketProps = markets(config, simulationProperties);
		Multimap<AgentType, Props> agentProps = agents(config, simulationProperties);
		Multiset<PlayerSpec> playerProps = players(players, simulationProperties);
		return new SimulationSpec(spec, simulationProperties, marketProps, agentProps, playerProps);
	}
	
	public static SimulationSpec create(Props simulationProperties, Multimap<MarketType, Props> marketProps,
			Multimap<AgentType, Props> agentProps, Multiset<PlayerSpec> playerProps) {
		return new SimulationSpec(new JsonObject(), simulationProperties, marketProps, agentProps, playerProps);
	}
	
	public static SimulationSpec create(JsonObject rawSpec, Props simulationProperties, Multimap<MarketType, Props> marketProps,
			Multimap<AgentType, Props> agentProps, Multiset<PlayerSpec> playerProps) {
		return new SimulationSpec(rawSpec, simulationProperties, marketProps, agentProps, playerProps);
	}
	
	public static SimulationSpec read(Reader reader) {
		return gsonReader.fromJson(reader, SimulationSpec.class);
	}
	
	public static SimulationSpec empty() {
		return new SimulationSpec();
	}

	protected static Props readProperties(JsonObject config) {
		Props.Builder builder = Props.builder();
		for (Entry<String, JsonElement> e : config.entrySet())
			if (!entityKeys.contains(e.getKey()))
				builder.put(e.getKey(), e.getValue().getAsString());
		return builder.build();
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
						split.hasNext() ? paramSplit.split(split.next()) : ImmutableList.<String> of())));
			}
		}
		return players.build();
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
	public static final class PlayerSpec {
		public final String descriptor;
		public final AgentType type;
		public final Props agentProps;
		
		public PlayerSpec(String role, String strategy, AgentType type, Props agentProps) {
			this.descriptor = role + ' ' + strategy;
			this.type = type;
			this.agentProps = agentProps;
		}
		
		@Override
		public String toString() {
			return descriptor + " " + type + " " + agentProps;
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
			return fromJson(json.getAsJsonObject());
		}
	}

	private static final long serialVersionUID = 5646083286397841102L;

}
