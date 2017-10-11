package edu.umich.srg.fourheap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;

import edu.umich.srg.testing.Repeat;
import edu.umich.srg.testing.RepeatRule;

import org.junit.Rule;
import org.junit.Test;

import java.util.Random;

public class ProRataSelectorTest {

  @Rule
  public RepeatRule repeatRule = new RepeatRule();

  private static final Random rand = new Random();

  /** Test that sizes match what they should be and all rounding is within one. */
  @Repeat(10000)
  @Test
  public void identityTest() {
    int size = rand.nextInt(100) + 100;
    int selectSize = rand.nextInt(size - 1) + 1;
    Multiset<Integer> initial = randomMultiset(size);
    assertEquals(size, initial.size());

    Multiset<Integer> copy = ImmutableMultiset.copyOf(initial);
    ProRataSelector<Integer> selector = ProRataSelector.create(rand);
    Multiset<Integer> selection = selector.select(initial, selectSize);

    assertEquals(selectSize, selection.size());
    assertEquals(size - selectSize, initial.size());

    double ratio = selectSize / (double) size;
    copy.entrySet().forEach(entry -> {
      int count = selection.count(entry.getElement());
      assertTrue(Math.abs(entry.getCount() * ratio - count) < 1 + 1e-6);
      assertTrue((0 <= count) && (count <= entry.getCount()));
    });

    initial.addAll(selection);
    assertEquals(copy, initial);
  }

  /** Test that identical seeds produce identical selections. */
  @Repeat(1000)
  @Test
  public void preservedRandomTest() {
    long seed = rand.nextLong();
    int size = rand.nextInt(100) + 100;
    int selectSize = rand.nextInt(size - 1) + 1;
    Multiset<Integer> initial1 = randomMultiset(size);
    Multiset<Integer> initial2 = HashMultiset.create(initial1);

    Random rand = new Random();
    ProRataSelector<Integer> selector = ProRataSelector.create(rand);

    rand.setSeed(seed);
    Multiset<Integer> selection1 = selector.select(initial1, selectSize);
    rand.setSeed(seed);
    Multiset<Integer> selection2 = selector.select(initial2, selectSize);

    assertEquals(initial1, initial2);
    assertEquals(selection1, selection2);
  }

  private static Multiset<Integer> randomMultiset(int size) {
    int sampleSize = Math.max(size / 2, 3);
    Multiset<Integer> result = HashMultiset.create();
    while (size > 0) {
      int add = Math.min(size, rand.nextInt(sampleSize));
      size -= add;
      result.add(rand.nextInt(sampleSize), add);
    }
    return result;
  }

}
