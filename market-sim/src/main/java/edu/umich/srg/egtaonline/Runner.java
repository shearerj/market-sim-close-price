package edu.umich.srg.egtaonline;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonStreamParser;

import com.github.rvesse.airline.SingleCommand;

import edu.umich.srg.egtaonline.Observation.Player;
import edu.umich.srg.egtaonline.SimSpec.RoleStrat;
import edu.umich.srg.util.SummStats;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Runner {

  private static final Charset charset = Charset.forName("UTF-8");

  /** Run an egta script with readers and writers. */
  private static void run(BiFunction<SimSpec, Integer, Observation> sim,
      Iterable<Entry<JsonObject, SimSpec>> specs, Consumer<Entry<JsonObject, Observation>> output,
      int numSims, int jobs) {
    checkArgument(numSims > 0, "total number of simulations must be greater than 0 (%d)", numSims);
    checkArgument(jobs >= 0, "number of jobs must be nonegative (%d)", jobs);

    if (jobs == 0) {
      jobs = Runtime.getRuntime().availableProcessors();
    }

    if (jobs > 1) {
      multiThreadRun(sim, specs, output, numSims, jobs);
    } else {
      singleThreadRun(sim, specs, output, numSims);
    }
  }


  /** Run an egta script with readers and writers. */
  public static void run(BiFunction<SimSpec, Integer, Observation> sim, Reader specs, Writer writer,
      int numObs, int simsPerObs, int jobs, boolean noFeatures, String classPrefix,
      CaseFormat keyCaseFormat) {

    boolean outputFeatures = simsPerObs == 1 && !noFeatures;
    SpecReader input = new SpecReader(specs, classPrefix, keyCaseFormat);
    Consumer<Entry<JsonObject, Observation>> output =
        createObsWriter(writer, simsPerObs, outputFeatures);

    run(sim, () -> input, output, numObs * simsPerObs, jobs);

    try {
      writer.flush();
    } catch (IOException ex) {
      ex.printStackTrace();
      System.exit(1);
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
    } else {
      try (Reader in = openin(options.simSpec); Writer out = openout(options.observations)) {
        run(sim, in, out, options.numObs, options.simsPerObs, options.jobs, options.noFeatures,
            classPrefix, keyCaseFormat);
      }
    }
  }

  private static void multiThreadRun(BiFunction<SimSpec, Integer, Observation> sim,
      Iterable<Entry<JsonObject, SimSpec>> specs, Consumer<Entry<JsonObject, Observation>> output,
      int numSims, int jobs) {
    try {
      /*
       * For unknown reasons (likely having to do with threads suppressing stderr, exceptions seem
       * to be hidden. Therefore almost everything is wrapped in a try{ } catch (Exception ex) {
       * e.printStackTrace(); System.exit(1); } to guarantee that the appropriate thing happens.
       */
      ExecutorService exec = Executors.newFixedThreadPool(jobs);

      int obsNum = 0;
      // We keep an ordered queue of finished observations that need to be written so that they come
      // out in order
      AtomicInteger nextObsToWrite = new AtomicInteger(0);
      PriorityQueue<Entry<Integer, Entry<JsonObject, Observation>>> pending =
          new PriorityQueue<>(Comparator.comparingInt(Entry::getKey));

      for (Entry<JsonObject, SimSpec> spec : specs) {
        for (int i = 0; i < numSims; ++i) {
          final int simNum = obsNum;
          exec.submit(() -> {
            // What's executed for every desired observation
            try {
              Observation obs = sim.apply(spec.getValue(), simNum);
              Entry<Integer, Entry<JsonObject, Observation>> res =
                  new AbstractMap.SimpleImmutableEntry<>(simNum,
                      new AbstractMap.SimpleImmutableEntry<>(spec.getKey(), obs));

              // Try to output everything from the queue
              synchronized (pending) {
                pending.add(res);
                while (!pending.isEmpty() && pending.peek().getKey() == nextObsToWrite.get()) {
                  output.accept(pending.poll().getValue());
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

  private static void singleThreadRun(BiFunction<SimSpec, Integer, Observation> sim,
      Iterable<Entry<JsonObject, SimSpec>> specs, Consumer<Entry<JsonObject, Observation>> output,
      int numSims) {

    int obsNum = 0;
    for (Entry<JsonObject, SimSpec> spec : specs) {
      for (int i = 0; i < numSims; ++i) {
        output.accept(new AbstractMap.SimpleImmutableEntry<>(spec.getKey(),
            sim.apply(spec.getValue(), obsNum)));
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
  private static final class SpecReader implements Iterator<Entry<JsonObject, SimSpec>> {
    private final JsonStreamParser parser;
    private final String classPrefix;
    private final CaseFormat keyCaseFormat;

    private SpecReader(Reader reader, String classPrefix, CaseFormat keyCaseFormat) {
      this.parser = new JsonStreamParser(reader);
      this.classPrefix = classPrefix;
      this.keyCaseFormat = keyCaseFormat;
    }

    @Override
    public boolean hasNext() {
      return parser.hasNext();
    }

    @Override
    public Entry<JsonObject, SimSpec> next() {
      JsonObject raw = parser.next().getAsJsonObject();
      return new AbstractMap.SimpleImmutableEntry<>(raw,
          SimSpec.read(raw, classPrefix, keyCaseFormat));
    }
  }

  private static Consumer<Entry<JsonObject, Observation>> createObsWriter(Writer output,
      int simsPerObs, boolean outputFeatures) {
    if (simsPerObs == 1 && outputFeatures) {
      // Output features one at a time
      Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
      return obs -> {
        JsonObject base = obs.getKey();
        base.add("features", obs.getValue().getFeatures());
        base.add("players", serializePlayers(obs.getValue().getPlayers(), true));
        gson.toJson(base, output);
        try {
          output.append('\n');
        } catch (IOException e) {
          e.printStackTrace();
          System.exit(1);
        }
      };
    } else {
      // Aggregate observation and don't output anything but payoffs
      Gson gson = new Gson();

      return new Consumer<Entry<JsonObject, Observation>>() {
        Multimap<RoleStrat, SummStats> aggregates = null;
        int numProcessed = 0;

        @Override
        public void accept(Entry<JsonObject, Observation> obs) {
          if (aggregates == null) {
            aggregates = ArrayListMultimap.create();
            for (Player player : obs.getValue().getPlayers()) {
              aggregates.put(RoleStrat.of(player.getRole(), player.getStrategy()),
                  SummStats.over(player.getPayoff()));
            }
          } else {
            // Iterator for each role strategy pair
            Map<RoleStrat, Iterator<SummStats>> next = aggregates.asMap().entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().iterator()));
            for (Player player : obs.getValue().getPlayers()) {
              next.get(RoleStrat.of(player.getRole(), player.getStrategy())).next()
                  .accept(player.getPayoff());
            }
          }
          numProcessed++;

          if (numProcessed >= simsPerObs) {
            JsonObject base = new JsonObject();
            base.add("players", serializeAggregatePlayers(aggregates, obs.getValue().getPlayers()));
            gson.toJson(base, output);
            try {
              output.append('\n');
            } catch (IOException e) {
              e.printStackTrace();
              System.exit(1);
            }
            aggregates = null;
            numProcessed = 0;
          }
        }
      };
    }
  }

  // Serializers

  private static JsonElement serializePlayers(Collection<? extends Player> players,
      boolean serializeFeatures) {
    JsonArray serializedPlayers = new JsonArray();
    for (Player player : players) {
      JsonObject serializedPlayer = new JsonObject();
      serializedPlayer.addProperty("role", player.getRole());
      serializedPlayer.addProperty("strategy", player.getStrategy());
      serializedPlayer.addProperty("payoff", player.getPayoff());
      JsonObject features;
      if (serializeFeatures && !(features = player.getFeatures()).entrySet().isEmpty()) {
        serializedPlayer.add("features", features);
      }
      serializedPlayers.add(serializedPlayer);
    }
    return serializedPlayers;
  }

  /** Serialize players, but keep the order of the last set of players. */
  private static JsonElement serializeAggregatePlayers(Multimap<RoleStrat, SummStats> players,
      Collection<? extends Player> order) {
    JsonArray serializedPlayers = new JsonArray();
    Map<RoleStrat, Iterator<SummStats>> next = players.asMap().entrySet().stream()
        .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().iterator()));

    for (Player player : order) {
      JsonObject serializedPlayer = new JsonObject();
      serializedPlayer.addProperty("role", player.getRole());
      serializedPlayer.addProperty("strategy", player.getStrategy());
      serializedPlayer.addProperty("payoff",
          next.get(RoleStrat.of(player.getRole(), player.getStrategy())).next().getAverage());
      serializedPlayers.add(serializedPlayer);
    }

    return serializedPlayers;
  }

}
