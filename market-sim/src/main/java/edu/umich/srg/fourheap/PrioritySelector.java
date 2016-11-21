package edu.umich.srg.fourheap;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Multisets;
import com.google.common.collect.Ordering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PrioritySelector<E> implements Selector<E> {

  private final Ordering<Entry<E>> order;

  private PrioritySelector(Ordering<? super E> ordering) {
    this.order = ordering.onResultOf(Entry::getElement);
  }

  public static <E extends Comparable<? super E>> PrioritySelector<E> create() {
    return new PrioritySelector<>(Ordering.natural());
  }

  public static <E> PrioritySelector<E> create(Comparator<? super E> comparator) {
    return new PrioritySelector<>(Ordering.from(comparator));
  }

  @Override
  public Multiset<E> select(Multiset<E> original, int quantity) {
    Multiset<E> result = HashMultiset.create();

    // This sorting guarantees consistent application of randomness at a slight performance penalty
    List<Entry<E>> entries = new ArrayList<>(original.entrySet());
    Collections.sort(entries, order);
    for (Entry<E> entry : entries) {
      int take = Math.min(entry.getCount(), quantity);
      quantity -= take;
      result.add(entry.getElement(), take);
      if (quantity == 0) {
        break;
      }
    }

    Multisets.removeOccurrences(original, result);
    return result;
  }

}
