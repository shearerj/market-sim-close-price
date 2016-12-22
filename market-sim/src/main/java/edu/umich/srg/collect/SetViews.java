package edu.umich.srg.collect;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class SetViews {

  /** Returns a view of the union of sets assuming they contain distinct elements. */
  @SuppressWarnings("unchecked")
  public static <E> Set<E> distinctUnion(Set<? extends E>... sets) {
    return new DistinctSetUnion<>(Arrays.asList(sets));
  }

  /** Returns a view of the union of sets assuming they contain distinct elements. */
  public static <E> Set<E> distinctUnion(Collection<? extends Set<? extends E>> sets) {
    return new DistinctSetUnion<>(sets);
  }

  private static class DistinctSetUnion<E> extends AbstractSet<E> {

    private final Collection<? extends Set<? extends E>> sets;

    private DistinctSetUnion(Collection<? extends Set<? extends E>> sets) {
      this.sets = sets;
    }

    @Override
    public boolean contains(Object obj) {
      return sets.stream().anyMatch(s -> s.contains(obj));
    }

    @Override
    public Iterator<E> iterator() {
      return sets.stream().<E>flatMap(Set::stream).iterator();
    }

    @Override
    public int size() {
      return sets.stream().mapToInt(Set::size).sum();
    }

  }

}
