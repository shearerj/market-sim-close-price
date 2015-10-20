package edu.umich.srg.egtaonline;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.stream.Stream;

import com.github.rvesse.airline.SingleCommand;
import com.google.common.base.CaseFormat;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import edu.umich.srg.collect.Iters;
import edu.umich.srg.collect.Iters.Enumerated;
import edu.umich.srg.collect.Streams;
import edu.umich.srg.egtaonline.Log.Level;
import edu.umich.srg.egtaonline.Observation.Player;
import edu.umich.srg.util.Functions.TriFunction;

public class Runner {

	private static final Gson
			jsonReader = new Gson(),
			egtaWriter = new GsonBuilder()
					.registerTypeAdapter(Player.class, new EgtaPlayerSerializer())
					.registerTypeAdapter(Observation.class, new EgtaObservationSerializer())
					.create(),
			fullWriter = new GsonBuilder()
					.registerTypeAdapter(Player.class, new FullPlayerSerializer())
					.registerTypeAdapter(Observation.class, new FullObservationSerializer())
					.serializeSpecialFloatingPointValues()
					.create();
	private static final Charset charset = Charset.forName("UTF-8");
	
	public static void run(TriFunction<SimSpec, Log, Long, Observation> sim, Reader specs, Writer obsOut,
			Writer logOut, int numObs, int intLogLevel, boolean egta, String classPrefix, CaseFormat keyCaseFormat) {
		
		Gson gson = egta ? egtaWriter : fullWriter;
		Level logLevel = Level.values()[Math.min(intLogLevel, Level.values().length - 1)];
		
		Stream<Enumerated<SimSpec>> specStream = Streams.stream(Iters
				.printExceptions(Iters.enumerate(Iters.repeat(Iters.map(fromJson(specs, JsonObject.class),
						(j) -> SimSpec.read(j, classPrefix, keyCaseFormat)), numObs))), true);
		specStream.map(c -> {
			try {
				StringWriter simLog = new StringWriter();
				Log log = Log.create(logLevel, simLog, (l) -> "");
				Observation obs = sim.apply(c.obj, log, c.index);
				return new Results(obs, simLog.toString());
			} catch (Exception ex) {
				ex.printStackTrace();
				throw ex;
			}
		}).forEachOrdered(r -> {
			try {
				gson.toJson(r.obs, Observation.class, obsOut);
				obsOut.write('\n');
				logOut.write(r.log);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	public static void run(TriFunction<SimSpec, Log, Long, Observation> sim, String[] args, String classPrefix,
			CaseFormat keyCaseFormat) throws IOException {
		SingleCommand<CommandLineOptions> parser = SingleCommand.singleCommand(CommandLineOptions.class);
		CommandLineOptions options = parser.parse(args);

		if (options.help.help) {
			options.help.showHelp();
		} else {
			try (Reader in = openin(options.simspec);
					Writer out = openout(options.observations);
					Writer log = openerr(options.logs)) {
				run(sim, in, out, log, options.numObs, options.verbosity, options.egta, classPrefix, keyCaseFormat);
			}
		}
	}

	private static Reader openin(String path) throws IOException {
		if (path == "-") {
			return new BufferedReader(new InputStreamReader(System.in, charset));
		} else {
			return Files.newBufferedReader(Paths.get(path), charset);
		}
	}

	private static Writer openout(String path) throws IOException {
		if (path == "-") {
			return new BufferedWriter(new OutputStreamWriter(System.out, charset));
		} else {
			return Files.newBufferedWriter(Paths.get(path), charset);
		}
	}

	private static Writer openerr(String path) throws IOException {
		if (path == "-") {
			return new BufferedWriter(new OutputStreamWriter(System.err, charset));
		} else {
			return Files.newBufferedWriter(Paths.get(path), charset);
		}
	}

	/** Convert a Reader into an iterator over JsonElements */
	private static <T extends JsonElement> Iterator<T> fromJson(Reader reader, final Class<T> clazz) {
		final JsonReader in = new JsonReader(reader);
		in.setLenient(true);
		return new Iterator<T>() {
			@Override
			public boolean hasNext() {
				try {
					return in.peek() != JsonToken.END_DOCUMENT;
				} catch (IOException e) {
					return false;
				}
			}

			@Override
			public T next() {
				return jsonReader.fromJson(in, clazz);
			}
		};
	}
	
	/** Combination of both outputs from a simulation */
	private static class Results {
		private Observation obs;
		private String log;
		
		private Results(Observation obs, String log) {
			this.obs = obs;
			this.log = log;
		}
	}
	
	// Serializers
	
	private static class EgtaPlayerSerializer implements JsonSerializer<Player> {

		@Override
		public JsonObject serialize(Player player, Type type, JsonSerializationContext gson) {
			JsonObject serializedPlayer = new JsonObject();
			serializedPlayer.addProperty("role", player.getRole());
			serializedPlayer.addProperty("strategy", player.getStrategy());
			serializedPlayer.addProperty("payoff", player.getPayoff());
			return serializedPlayer;
		}
		
	}
	
	private static class FullPlayerSerializer extends EgtaPlayerSerializer {

		@Override
		public JsonObject serialize(Player player, Type type, JsonSerializationContext gson) {
			JsonObject serializedPlayer = super.serialize(player, type, gson);
			JsonObject features = player.getFeatures();
			if (!features.entrySet().isEmpty())
				serializedPlayer.add("features", features);
			return serializedPlayer;
		}
		
	}
	
	private static class EgtaObservationSerializer implements JsonSerializer<Observation> {

		@Override
		public JsonObject serialize(Observation observation, Type type, JsonSerializationContext gson) {
			JsonObject serializedObservation = new JsonObject();
			JsonArray players = new JsonArray();
			serializedObservation.add("players", players);
			for (Player player : observation.getPlayers())
				players.add(gson.serialize(player, Player.class));
			return serializedObservation;
		}
		
	}
	
	private static class FullObservationSerializer extends EgtaObservationSerializer {

		@Override
		public JsonObject serialize(Observation observation, Type type, JsonSerializationContext gson) {
			JsonObject serializedObservation = super.serialize(observation, type, gson);
			JsonObject features = observation.getFeatures();
			if (!features.entrySet().isEmpty())
				serializedObservation.add("features", features);
			return serializedObservation;
		}
		
	}

}
