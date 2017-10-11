package edu.umich.srg.fourheap;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import org.junit.Ignore;
import org.junit.Test;

import java.util.Random;
import java.util.stream.IntStream;

@Ignore
public class RandProRataSelectorTest {

  private static final Random rand = new Random();

  @Test
  public void uniformTest() {
    int size = 5;
    int num = 100000;
    Multiset<Integer> base = HashMultiset.create(size);
    IntStream.range(0, size).forEach(i -> base.add(i, i + 1));
    ProRataSelector<Integer> selector = ProRataSelector.create(rand);
    double[] average = new double[size];

    IntStream.rangeClosed(1, num).forEach(i -> {
      Multiset<Integer> selection = selector.select(HashMultiset.create(base), size + 1);
      IntStream.range(0, size)
          .forEach(val -> average[val] += (selection.count(val) - average[val]) / i);
    });

    IntStream.range(0, size).forEach(i -> {
      double expected = (i + 1) * 2 / (double) size;
      double stddev = Math.sqrt((expected % 1) * (1 - expected % 1) / num);
      assertEquals(expected, average[i], 3 * stddev);
    });
  }

}
