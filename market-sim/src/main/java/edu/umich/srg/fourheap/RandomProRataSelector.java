package edu.umich.srg.fourheap;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Multisets;
import com.google.common.collect.Ordering;

import edu.umich.srg.distributions.Hypergeometric;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Select elements to keep uniformly random such that the quantity is still selected. In expectation
 * `element.count() * quantity / original.size()` will be selected from each element, but it could
 * be any feasible number. The advantage of this implementation is that large orders are treated
 * identically to small orders, whereas in the reduced variance version, large orders have reduced
 * variance. If all orders are the same size, this is identical to ProRataSelector.
 */
public class RandomProRataSelector<E> implements Selector<E> {

  private final Ordering<Entry<E>> order;
  private final Random rand;

  private RandomProRataSelector(Ordering<? super E> ordering, Random rand) {
    this.order = Ordering.natural().onResultOf(Entry<E>::getCount)
        .compound(ordering.onResultOf(Entry::getElement));
    this.rand = rand;
  }

  public static <E extends Comparable<? super E>> RandomProRataSelector<E> create(Random rand) {
    return new RandomProRataSelector<>(Ordering.natural(), rand);
  }

  public static <E> RandomProRataSelector<E> create(Comparator<? super E> comparator, Random rand) {
    return new RandomProRataSelector<>(Ordering.from(comparator), rand);
  }

  @Override
  public Multiset<E> select(Multiset<E> original, int quantity) {
    Multiset<E> result = HashMultiset.create();
    int total = original.size();

    // This sorting guarantees consistent application of randomness at a slight performance penalty
    List<Entry<E>> entries = order.immutableSortedCopy(original.entrySet());
    for (Entry<E> entry : entries) {
      int sample = Hypergeometric.with(total, entry.getCount(), quantity).sample(rand);
      total -= entry.getCount();
      quantity -= sample;
      result.add(entry.getElement(), sample);
    }

    Multisets.removeOccurrences(original, result);
    return result;
  }

}
