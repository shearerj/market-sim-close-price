package edu.umich.srg.fourheap;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Multisets;
import com.google.common.collect.Ordering;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Select elements to keep the number from each element close to `element.count() * quantity /
 * original.size()` (i.e. within 1). Any fractional account that can't be guaranteed will be decided
 * randomly under the constraint the correct number is still added.
 */
public class ProRataSelector<E> implements Selector<E> {

  private final Ordering<Entry<E>> order;
  private final Random rand;

  private ProRataSelector(Comparator<? super E> comparator, Random rand) {
    this.order = Ordering.natural().onResultOf(Entry<E>::getCount)
        .compound(Ordering.from(comparator).onResultOf(Entry<E>::getElement));
    this.rand = rand;
  }

  public static <E extends Comparable<? super E>> ProRataSelector<E> create(Random rand) {
    return new ProRataSelector<>(Ordering.natural(), rand);
  }

  public static <E> ProRataSelector<E> create(Comparator<? super E> comparator, Random rand) {
    return new ProRataSelector<>(Ordering.from(comparator), rand);
  }

  @Override
  public Multiset<E> select(Multiset<E> original, int quantity) {
    Multiset<E> result = HashMultiset.create();
    int total = original.size();
    double ratio = quantity / (double) total;

    // This sorting guarantees consistent application of randomness at a slight performance penalty
    List<Entry<E>> entries = order.sortedCopy(original.entrySet());
    Collections.shuffle(entries, rand);
    Iterator<Entry<E>> iter = entries.iterator();

    Entry<E> current = iter.next();
    double residual = (current.getCount() * ratio) % 1;
    // System.err.printf("%d: %f\n", current.getElement(), residual);

    /*
     * At each step we guarantee that the sum of `inc` and the total residual remains constant, and
     * in expectation, each residual equals the probability of incimenting.
     */
    while (iter.hasNext()) {
      Entry<E> next = iter.next();
      double nextResid = (next.getCount() * ratio) % 1;
      boolean inc = 1 < residual + nextResid;
      double newResid = residual + nextResid - (inc ? 1 : 0);
      double keepProb = inc ? (1 - nextResid) / (1 - newResid) : nextResid / newResid;
      // System.err.printf("(%d %d): (%f %f -> %f) %b %f\n", current.getElement(),
      // next.getElement(),
      // residual, nextResid, newResid, inc, keepProb);
      if (rand.nextDouble() < keepProb) {
        Entry<E> temp = next;
        next = current;
        current = temp;
      }
      residual = newResid;
      result.add(next.getElement(), (int) (next.getCount() * ratio) + (inc ? 1 : 0));
    }

    // The final residual will be close to zero or one
    result.add(current.getElement(), (int) (current.getCount() * ratio) + (0.5 < residual ? 1 : 0));

    Multisets.removeOccurrences(original, result);
    return result;
  }

}
