package edu.umich.srg.fourheap;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;

import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.stream.IntStream;

@Ignore
public class RandomConsistentRandomSelectorTest {

  private static final Random rand = new Random();

  @Test
  public void uniformTest1() {
    int size = 5;
    int num = 100000;
    Multiset<Integer> base = ImmutableMultiset.copyOf(IntStream.range(0, size).boxed().iterator());
    ConsistentRandomSelector<Integer> selector = ConsistentRandomSelector.create(rand);
    int[] counts = new int[size];

    for (int i = 0; i < num; ++i) {
      int sample = Iterables.getFirst(selector.select(HashMultiset.create(base), 1), null);
      ++counts[sample];
    }

    Arrays.stream(counts).forEach(c -> {
      double stddev = Math.sqrt(((size - 1) * num) / (double) (size * size));
      assertEquals(num / (double) size, c, 3 * stddev);
    });
  }

  @Test
  public void uniformTest2() {
    int scale = 3;
    int size = 5;
    int num = 100000;
    Multiset<Integer> base = ImmutableMultiset
        .copyOf(IntStream.range(0, size * scale).map(x -> x / scale).boxed().iterator());
    ConsistentRandomSelector<Integer> selector = ConsistentRandomSelector.create(rand);
    int[] counts = new int[size];

    for (int i = 0; i < num; ++i) {
      Collection<Integer> selection = selector.select(HashMultiset.create(base), scale);
      for (int x : selection) {
        ++counts[x];
      }
    }

    Arrays.stream(counts).forEach(c -> {
      double stddev = Math.sqrt(((size - 1) * num * scale) / (double) (size * size));
      assertEquals(num * scale / (double) size, c, 3 * stddev);
    });
  }

}
