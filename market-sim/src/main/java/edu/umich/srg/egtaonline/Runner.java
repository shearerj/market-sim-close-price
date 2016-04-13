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

import edu.umich.srg.egtaonline.Observation.Player;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

public class Runner {

  private static final Gson egtaWriter =
      new GsonBuilder().registerTypeAdapter(Player.class, new EgtaPlayerSerializer())
          .registerTypeAdapter(Observation.class, new EgtaObservationSerializer()).create();
  private static final Gson fullWriter =
      new GsonBuilder().registerTypeAdapter(Player.class, new FullPlayerSerializer())
          .registerTypeAdapter(Observation.class, new FullObservationSerializer())
          .serializeSpecialFloatingPointValues().create();
  private static final Charset charset = Charset.forName("UTF-8");

  /** Run an egta script. */
  public static void run(BiFunction<SimSpec, Integer, Observation> sim, Reader specs, Writer obsOut,
      int numObs, int jobs, boolean egta, String classPrefix, CaseFormat keyCaseFormat) {

    Gson gson = egta ? egtaWriter : fullWriter;
    if (jobs == 0) {
      jobs = Runtime.getRuntime().availableProcessors();
    }

    if (jobs > 1) {
      multiThreadRun(sim, specs, obsOut, numObs, jobs, gson, classPrefix, keyCaseFormat);
    } else {
      singleThreadRun(sim, specs, obsOut, numObs, gson, classPrefix, keyCaseFormat);
    }
    try {
      obsOut.flush();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  /** Run an egta script with command line arguments. */
  public static void run(BiFunction<SimSpec, Integer, Observation> sim, String[] args,
      String classPrefix, CaseFormat keyCaseFormat) throws IOException {
    SingleCommand<CommandLineOptions> parser =
        SingleCommand.singleCommand(CommandLineOptions.class);
    CommandLineOptions options = parser.parse(args);

    if (options.help.help) {
      options.help.showHelp();
      // FIXME Not properly exiting after help
    } else {
      try (Reader in = openin(options.simspec); Writer out = openout(options.observations)) {
        run(sim, in, out, options.numObs, options.jobs, options.egta, classPrefix, keyCaseFormat);
      }
    }
  }

  private static void multiThreadRun(BiFunction<SimSpec, Integer, Observation> sim, Reader specs,
      Writer obsOut, int numObs, int jobs, Gson gson, String classPrefix,
      CaseFormat keyCaseFormat) {
    try {
      /*
       * For unknown reasons (likely having to do with threads suppressing stderr, exceptions seem
       * to be hidden. Therefore almost everything is wrapped in a try{ } catch (Exception ex) {
       * e.printStackTrace(); System.exit(1); } to guarantee that the appropriate thing happens.
       */
      ExecutorService exec = Executors.newFixedThreadPool(jobs);

      JsonStreamParser parser = new JsonStreamParser(specs);
      int obsNum = 0;
      // We keep an ordered queue of finished observations that need to be written so that they come
      // out in order
      AtomicInteger nextObsToWrite = new AtomicInteger(0);
      PriorityQueue<Result> pending = new PriorityQueue<>();

      while (parser.hasNext()) {
        SimSpec spec = SimSpec.read(parser.next().getAsJsonObject(), classPrefix, keyCaseFormat);
        for (int i = 0; i < numObs; ++i) {
          int finalObsNum = obsNum; // final
          exec.submit(() -> {
            // What's executed for every desired observation
            try {
              Observation obs = sim.apply(spec, finalObsNum);
              Result res = new Result(finalObsNum, obs);

              // Try to output everything from the queue
              synchronized (pending) {
                pending.add(res);
                while (!pending.isEmpty() && pending.peek().obsNum == nextObsToWrite.get()) {
                  Result toWrite = pending.poll();

                  gson.toJson(toWrite.obs, Observation.class, obsOut);
                  obsOut.write('\n');
                  nextObsToWrite.incrementAndGet();
                }
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

  private static void singleThreadRun(BiFunction<SimSpec, Integer, Observation> sim, Reader specs,
      Writer obsOut, int numObs, Gson gson, String classPrefix,
      CaseFormat keyCaseFormat) {

    JsonStreamParser parser = new JsonStreamParser(specs);
    int obsNum = 0;
    while (parser.hasNext()) {
      SimSpec spec = SimSpec.read(parser.next().getAsJsonObject(), classPrefix, keyCaseFormat);
      for (int i = 0; i < numObs; ++i) {
        Observation obs = sim.apply(spec, obsNum);
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

  // Run result, necessary for synchronization

  private static class Result implements Comparable<Result> {
    private final int obsNum;
    private final Observation obs;

    private Result(int obsNum, Observation obs) {
      this.obsNum = obsNum;
      this.obs = obs;
    }

    @Override
    public int compareTo(Result that) {
      return Integer.compare(this.obsNum, that.obsNum);
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
      if (!features.entrySet().isEmpty()) {
        serializedPlayer.add("features", features);
      }
      return serializedPlayer;
    }

  }

  private static class EgtaObservationSerializer implements JsonSerializer<Observation> {

    @Override
    public JsonObject serialize(Observation observation, Type type, JsonSerializationContext gson) {
      JsonObject serializedObservation = new JsonObject();
      JsonArray players = new JsonArray();
      serializedObservation.add("players", players);
      for (Player player : observation.getPlayers()) {
        players.add(gson.serialize(player, Player.class));
      }
      return serializedObservation;
    }

  }

  private static class FullObservationSerializer extends EgtaObservationSerializer {

    @Override
    public JsonObject serialize(Observation observation, Type type, JsonSerializationContext gson) {
      JsonObject serializedObservation = super.serialize(observation, type, gson);
      JsonObject features = observation.getFeatures();
      if (!features.entrySet().isEmpty()) {
        serializedObservation.add("features", features);
      }
      return serializedObservation;
    }

  }

}
