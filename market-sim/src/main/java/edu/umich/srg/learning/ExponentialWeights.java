package edu.umich.srg.learning;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Ints;

import edu.umich.srg.distributions.Distribution;
import edu.umich.srg.distributions.Multinomial;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.stream.Collectors;

public class ExponentialWeights<T> implements Distribution<T> {

  final Map<T, Double> weights;
  private final double learningRate;
  private final double logN;
  private int time;
  private Multinomial<T> distribution;

  private ExponentialWeights(double learningRate, Iterator<T> experts) {
    this.weights = new LinkedHashMap<>();
    for (T expert : (Iterable<T>) () -> experts) {
      weights.put(expert, 1d);
    }
    checkArgument(!weights.isEmpty());
    normalizeWeights(weights.size());

    this.learningRate = learningRate;
    this.logN = Math.log(weights.size());
    this.time = 1;
    this.distribution = null;
  }

  public static <T> ExponentialWeights<T> create(double learningRate, Iterator<T> experts) {
    return new ExponentialWeights<>(learningRate, experts);
  }

  public static <T> ExponentialWeights<T> create(double learningRate, Iterable<T> experts) {
    return new ExponentialWeights<>(learningRate, experts.iterator());
  }

  public static <T extends Number> NumericExponentialWeights<T> createNumeric(double learningRate,
      Iterator<T> experts) {
    return new NumericExponentialWeights<>(learningRate, experts);
  }

  public static <T extends Number> NumericExponentialWeights<T> createNumeric(double learningRate,
      Iterable<T> experts) {
    return new NumericExponentialWeights<>(learningRate, experts.iterator());
  }

  private void normalizeWeights(double total) {
    for (Entry<T, Double> e : weights.entrySet()) {
      e.setValue(1d / total);
    }
  }

  private void internalUpdate(Iterator<Entry<Entry<T, Double>, ? extends Number>> entrysAndGains) {
    double eta = learningRate * Math.min(1, Math.sqrt(logN / time));
    double total = 0;
    while (entrysAndGains.hasNext()) {
      Entry<Entry<T, Double>, ? extends Number> entry = entrysAndGains.next();
      double newWeight = entry.getKey().getValue() * Math.exp(eta * entry.getValue().doubleValue());
      entry.getKey().setValue(newWeight);
      total += newWeight;
    }
    normalizeWeights(total);
    time++;
    distribution = null;
  }

  /** Update the exponential weights algorithm with gains in insertion order */
  public void update(Iterator<? extends Number> gains) {
    Iterator<Entry<T, Double>> experts = weights.entrySet().iterator();
    internalUpdate(new Iterator<Entry<Entry<T, Double>, ? extends Number>>() {

      @Override
      public boolean hasNext() {
        if (gains.hasNext() ^ experts.hasNext()) { // They disagree
          throw new IllegalArgumentException(
              "Update set must have the same number of experts as the original map");
        } else {
          return experts.hasNext();
        }
      }

      @Override
      public Entry<Entry<T, Double>, ? extends Number> next() {
        return new AbstractMap.SimpleImmutableEntry<>(experts.next(), gains.next());
      }

    });
  }

  /** Update the exponential weights algorithm with gains in insertion order */
  public void update(Iterable<? extends Number> gains) {
    update(gains.iterator());
  }

  /** Update the exponential weights algorithm with gains with a mapping */
  public void update(Map<T, ? extends Number> mappedGains) {
    Iterator<? extends Entry<T, ? extends Number>> gains = mappedGains.entrySet().iterator();
    internalUpdate(new Iterator<Entry<Entry<T, Double>, ? extends Number>>() {

      @Override
      public boolean hasNext() {
        return gains.hasNext();
      }

      @Override
      public Entry<Entry<T, Double>, ? extends Number> next() {
        Entry<T, ? extends Number> gain = gains.next();
        return new AbstractMap.SimpleImmutableEntry<>(
            new AbstractMap.SimpleImmutableEntry<>(gain.getKey(), weights.get(gain.getKey())),
            gain.getValue());
      }

    });
  }

  /** Return a probability / weight for each expert */
  public Map<T, Double> getProbabilities() {
    return Collections.unmodifiableMap(weights);
  }

  /** Get counts for strategies that sum to total according to weights */
  public Map<T, Integer> getCounts(int total, Random rand) {
    double[] newWeights = new double[weights.size()];
    int[] counts = new int[weights.size()];

    // Multiply weights but total counts and remove any integer parts
    int countLeft = total;
    int i = 0;
    for (double weight : weights.values()) {
      weight *= total;
      int count = (int) Math.floor(weight);
      countLeft -= count;
      counts[i] = count;
      newWeights[i] = weight - count;
      i++;
    }

    // Normalize by number of times we'll sample
    for (i = 0; i < newWeights.length; ++i) {
      newWeights[i] /= countLeft;
    }

    // Sample remaining ints
    IntDistribution extras = Multinomial.withWeights(newWeights);
    for (i = 0; i < countLeft; ++i) {
      counts[extras.sample(rand)]++;
    }

    // Build map of results
    ImmutableMap.Builder<T, Integer> mappedCounts = ImmutableMap.builder();
    Iterator<T> elements = weights.keySet().iterator();
    for (i = 0; i < counts.length; ++i) {
      mappedCounts.put(elements.next(), counts[i]);
    }
    return mappedCounts.build();
  }

  /** Return an expert with probability proportional to it's weight */
  @Override
  public T sample(Random rand) {
    if (distribution == null) {
      distribution = Multinomial.fromMap(weights);
    }
    return distribution.sample(rand);
  }

  public static class NumericExponentialWeights<T extends Number> extends ExponentialWeights<T> {
    private NumericExponentialWeights(double learningRate, Iterator<T> experts) {
      super(learningRate, experts);
    }

    /** Returns the expected expert value */
    public double expectedValue() {
      return weights.entrySet().stream().mapToDouble(e -> e.getKey().doubleValue() * e.getValue())
          .sum();
    }
  }

  public static void main(String[] args) {
    Random rand = new Random();
    ExponentialWeights<Integer> x = new ExponentialWeights<>(1, Ints.asList(0, 1, 2).iterator());
    x.weights.put(0, 0.2);
    x.weights.put(1, 0.3);
    x.weights.put(2, 0.5);
    int[] counts = new int[3];
    int n = 10000, m = 6;
    for (int i = 0; i < n; ++i) {
      for (Entry<Integer, Integer> e : x.getCounts(m, rand).entrySet()) {
        counts[e.getKey()] += e.getValue();
      }
    }
    System.out.println(Arrays.stream(counts).mapToDouble(c -> c / (double) (m * n)).boxed()
        .collect(Collectors.toList()));
  }

}
