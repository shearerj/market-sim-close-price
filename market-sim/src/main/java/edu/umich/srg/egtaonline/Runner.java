package edu.umich.srg.egtaonline;

import com.google.common.base.CaseFormat;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonStreamParser;

import com.github.rvesse.airline.SingleCommand;

import edu.umich.srg.egtaonline.Log.Level;
import edu.umich.srg.egtaonline.Observation.Player;
import edu.umich.srg.util.Functions.TriFunction;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Runner {

  private static final Gson egtaWriter =
      new GsonBuilder().registerTypeAdapter(Player.class, new EgtaPlayerSerializer())
          .registerTypeAdapter(Observation.class, new EgtaObservationSerializer()).create(),
      fullWriter = new GsonBuilder().registerTypeAdapter(Player.class, new FullPlayerSerializer())
          .registerTypeAdapter(Observation.class, new FullObservationSerializer())
          .serializeSpecialFloatingPointValues().create();
  private static final Charset charset = Charset.forName("UTF-8");

  public static void run(TriFunction<SimSpec, Log, Integer, Observation> sim, Reader specs,
      Writer obsOut, Writer logOut, int numObs, int intLogLevel, int jobs, boolean egta,
      String classPrefix, CaseFormat keyCaseFormat) {

    Gson gson = egta ? egtaWriter : fullWriter;
    Level logLevel = Level.values()[Math.min(intLogLevel, Level.values().length - 1)];
    if (jobs == 0) {
      jobs = Runtime.getRuntime().availableProcessors();
    }

    if (jobs > 1) {
      multiThreadRun(sim, specs, obsOut, logOut, numObs, jobs, logLevel, gson, classPrefix,
          keyCaseFormat);
    } else {
      singleThreadRun(sim, specs, obsOut, logOut, numObs, logLevel, gson, classPrefix,
          keyCaseFormat);
    }
    try {
      obsOut.flush();
      logOut.flush();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  private static void multiThreadRun(TriFunction<SimSpec, Log, Integer, Observation> sim,
      Reader specs, Writer obsOut, Writer logOut, int numObs, int jobs, Level logLevel, Gson gson,
      String classPrefix, CaseFormat keyCaseFormat) {
    try {
      /*
       * For unknown reasons (likely having to do with threads suppressing stderr, exceptions seem
       * to be hidden. Therefore almost everything is wrapped in a try{ } catch (Exception ex) {
       * e.printStackTrace(); System.exit(1); } to guarantee that the appropriate thing happens.
       */

      ExecutorService exec = Executors.newFixedThreadPool(jobs);

      JsonStreamParser parser = new JsonStreamParser(specs);
      int obsNum = 0;
      while (parser.hasNext()) {
        SimSpec spec = SimSpec.read(parser.next().getAsJsonObject(), classPrefix, keyCaseFormat);
        for (int i = 0; i < numObs; ++i) {
          int finalObsNum = obsNum; // final
          exec.submit(() -> {
            // What's executed for every desired observation
            try {
              StringWriter logWriter = new StringWriter();
              Log log = Log.create(logLevel, logWriter, l -> "");
              Observation obs = sim.apply(spec, log, finalObsNum);

              synchronized (obsOut) {
                gson.toJson(obs, Observation.class, obsOut);
                obsOut.write('\n');
                logOut.write(logWriter.toString());
              }
            } catch (Exception ex) {
              ex.printStackTrace();
              System.exit(1);
            }
          });

          ++obsNum;
        }
      }

      // Wait for all workers to finish
      exec.shutdown();

      exec.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    } catch (Exception ex) {
      ex.printStackTrace();
      System.exit(1);
    }
  }

  private static void singleThreadRun(TriFunction<SimSpec, Log, Integer, Observation> sim,
      Reader specs, Writer obsOut, Writer logOut, int numObs, Level logLevel, Gson gson,
      String classPrefix, CaseFormat keyCaseFormat) {

    Log log = Log.create(logLevel, logOut, l -> "");

    JsonStreamParser parser = new JsonStreamParser(specs);
    int obsNum = 0;
    while (parser.hasNext()) {
      SimSpec spec = SimSpec.read(parser.next().getAsJsonObject(), classPrefix, keyCaseFormat);
      for (int i = 0; i < numObs; ++i) {
        Observation obs = sim.apply(spec, log, obsNum);
        gson.toJson(obs, Observation.class, obsOut);
        try {
          obsOut.write('\n');
        } catch (IOException e) {
          e.printStackTrace();
        }
        ++obsNum;
      }
    }
  }

  public static void run(TriFunction<SimSpec, Log, Integer, Observation> sim, String[] args,
      String classPrefix, CaseFormat keyCaseFormat) throws IOException {
    SingleCommand<CommandLineOptions> parser =
        SingleCommand.singleCommand(CommandLineOptions.class);
    CommandLineOptions options = parser.parse(args);

    if (options.help.help) {
      options.help.showHelp();
      // FIXME Not properly exiting after help
    } else {
      try (Reader in = openin(options.simspec);
          Writer out = openout(options.observations);
          Writer log = openerr(options.logs)) {
        run(sim, in, out, log, options.numObs, options.verbosity, options.jobs, options.egta,
            classPrefix, keyCaseFormat);
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
