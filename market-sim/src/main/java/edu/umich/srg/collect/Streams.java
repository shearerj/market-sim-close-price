package edu.umich.srg.collect;

import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Streams {

  /** Convert an iterator into a stream */
  public static <T> Stream<T> stream(Iterator<T> iterator) {
    return StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(Objects.requireNonNull(iterator), Spliterator.ORDERED),
        false);
  }

  /** Zip two streams together with a zip function */
  public static <A, B, C> Stream<C> zip(Stream<? extends A> a, Stream<? extends B> b,
      BiFunction<? super A, ? super B, ? extends C> zipper) {
    Objects.requireNonNull(zipper);
    Spliterator<? extends A> aSpliterator = Objects.requireNonNull(a).spliterator();
    Spliterator<? extends B> bSpliterator = Objects.requireNonNull(b).spliterator();

    // Zipping looses DISTINCT and SORTED characteristics
    int characteristics = aSpliterator.characteristics() & bSpliterator.characteristics()
        & ~(Spliterator.DISTINCT | Spliterator.SORTED);
    long zipSize = ((characteristics & Spliterator.SIZED) != 0)
        ? Math.min(aSpliterator.getExactSizeIfKnown(), bSpliterator.getExactSizeIfKnown()) : -1;

    Iterator<? extends A> aIterator = Spliterators.iterator(aSpliterator);
    Iterator<? extends B> bIterator = Spliterators.iterator(bSpliterator);
    Iterator<C> cIterator = new Iterator<C>() {
      @Override
      public boolean hasNext() {
        return aIterator.hasNext() && bIterator.hasNext();
      }

      @Override
      public C next() {
        return zipper.apply(aIterator.next(), bIterator.next());
      }
    };

    Spliterator<C> split = Spliterators.spliterator(cIterator, zipSize, characteristics);
    return StreamSupport.stream(split, a.isParallel() && b.isParallel());
  }

}
