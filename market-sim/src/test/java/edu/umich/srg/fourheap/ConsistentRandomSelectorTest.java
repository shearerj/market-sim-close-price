package edu.umich.srg.fourheap;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;

import org.junit.Rule;
import org.junit.Test;

import edu.umich.srg.testing.Repeat;
import edu.umich.srg.testing.RepeatRule;

import java.util.Random;

public class ConsistentRandomSelectorTest {

  @Rule
  public RepeatRule repeatRule = new RepeatRule();

  private static final Random rand = new Random();

  @Repeat(100)
  @Test
  public void identityTest() {
    int size = rand.nextInt(100) + 100;
    int selectSize = rand.nextInt(size - 1) + 1;
    Multiset<Integer> initial = randomMultiset(size);
    assertEquals(size, initial.size());

    Multiset<Integer> copy = ImmutableMultiset.copyOf(initial);
    ConsistentRandomSelector<Integer> selector = ConsistentRandomSelector.create(rand);
    Multiset<Integer> selection = selector.select(initial, selectSize);

    assertEquals(size - selectSize, initial.size());
    assertEquals(selectSize, selection.size());

    initial.addAll(selection);
    assertEquals(copy, initial);
  }

  @Repeat(100)
  @Test
  public void preservedRandomTest() {
    long seed = rand.nextLong();
    int size = rand.nextInt(100) + 100;
    int selectSize = rand.nextInt(size - 1) + 1;
    Multiset<Integer> initial1 = randomMultiset(size);
    Multiset<Integer> initial2 = HashMultiset.create(initial1);

    Random rand = new Random();
    ConsistentRandomSelector<Integer> selector = ConsistentRandomSelector.create(rand);

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
