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
import java.util.Collection;
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
  private static void run(BiFunction<SimSpec, Integer, Observation> sim, Iterable<JSpec> specs,
      Consumer<JObs> output, int numSims, int jobs) {
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
    Consumer<JObs> output = createObsWriter(writer, simsPerObs, outputFeatures);

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
      Iterable<JSpec> specs, Consumer<JObs> output, int numSims, int jobs) {
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
      PriorityQueue<Result> pending = new PriorityQueue<>();

      for (JSpec spec : specs) {
        for (int i = 0; i < numSims; ++i) {
          final int simNum = obsNum;
          exec.submit(() -> {
            // What's executed for every desired observation
            try {
              Observation obs = sim.apply(spec.spec, simNum);
              Result res = new Result(simNum, obs, spec.raw);

              // Try to output everything from the queue
              synchronized (pending) {
                pending.add(res);
                while (!pending.isEmpty() && pending.peek().obsNum == nextObsToWrite.get()) {
                  output.accept(pending.poll().obs);
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
      Iterable<JSpec> specs, Consumer<JObs> output, int numSims) {

    int obsNum = 0;
    for (JSpec spec : specs) {
      for (int i = 0; i < numSims; ++i) {
        output.accept(new JObs(spec.raw, sim.apply(spec.spec, obsNum)));
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
    private final JObs obs;

    private Result(int obsNum, Observation obs, JsonObject raw) {
      this.obsNum = obsNum;
      this.obs = new JObs(raw, obs);
    }

    @Override
    public int compareTo(Result that) {
      return Integer.compare(this.obsNum, that.obsNum);
    }

  }

  private static final class JSpec {
    private final JsonObject raw;
    private final SimSpec spec;

    private JSpec(JsonObject raw, SimSpec spec) {
      this.raw = raw;
      this.spec = spec;
    }
  }

  private static final class JObs {
    private final JsonObject raw;
    private final Observation obs;

    private JObs(JsonObject raw, Observation obs) {
      this.raw = raw;
      this.obs = obs;
    }
  }

  private static final class SpecReader implements Iterator<JSpec> {
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
    public JSpec next() {
      JsonObject raw = parser.next().getAsJsonObject();
      return new JSpec(raw, SimSpec.read(raw, classPrefix, keyCaseFormat));
    }
  }

  private static Consumer<JObs> createObsWriter(Writer output, int simsPerObs,
      boolean outputFeatures) {
    if (simsPerObs == 1 && outputFeatures) {
      // Output features one at a time
      Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
      return obs -> {
        JsonObject base = obs.raw;
        base.add("features", obs.obs.getFeatures());
        base.add("players", serializePlayers(obs.obs.getPlayers(), true));
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

      return new Consumer<JObs>() {
        Multimap<RoleStrat, SummStats> aggregates = null;
        int numProcessed = 0;

        @Override
        public void accept(JObs obs) {
          if (aggregates == null) {
            aggregates = ArrayListMultimap.create();
            for (Player player : obs.obs.getPlayers()) {
              aggregates.put(RoleStrat.of(player.getRole(), player.getStrategy()),
                  SummStats.over(player.getPayoff()));
            }
          } else {
            // Iterator for each role strategy pair
            Map<RoleStrat, Iterator<SummStats>> next = aggregates.asMap().entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().iterator()));
            for (Player player : obs.obs.getPlayers()) {
              next.get(RoleStrat.of(player.getRole(), player.getStrategy())).next()
                  .accept(player.getPayoff());
            }
          }
          numProcessed++;

          if (numProcessed >= simsPerObs) {
            JsonObject base = new JsonObject();
            base.add("players", serializeAggregatePlayers(aggregates));
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

  private static JsonElement serializeAggregatePlayers(Multimap<RoleStrat, SummStats> players) {
    JsonArray serializedPlayers = new JsonArray();
    for (Entry<RoleStrat, SummStats> player : players.entries()) {
      JsonObject serializedPlayer = new JsonObject();
      serializedPlayer.addProperty("role", player.getKey().getRole());
      serializedPlayer.addProperty("strategy", player.getKey().getStrategy());
      serializedPlayer.addProperty("payoff", player.getValue().getAverage());
      serializedPlayers.add(serializedPlayer);
    }
    return serializedPlayers;
  }

}
