package edu.umich.srg.collect;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.stream.Stream;

public class SparseArrayList<E> implements SparseList<E>, Serializable {

  private final ArrayList<MutableEntry> elements;

  private SparseArrayList() {
    elements = new ArrayList<>();
  }

  public static <E> SparseArrayList<E> empty() {
    return new SparseArrayList<>();
  }

  @Override
  public void add(long index, E element) {
    if (elements.isEmpty()) {
      elements.add(new MutableEntry(index, element));
      return;
    }
    MutableEntry last = elements.get(elements.size() - 1);
    if (last.getIndex() == index) {
      last.element = element;
      return;
    } else if (last.getIndex() < index) {
      elements.add(new MutableEntry(index, element));
      return;
    }
    int location = Collections.binarySearch(elements, SparseList.immutableEntry(index, null),
        (a, b) -> Long.compare(a.getIndex(), b.getIndex()));
    if (location >= 0) {
      elements.get(location).element = element;
    } else {
      elements.add(-location - 1, new MutableEntry(index, element));
    }
  }

  @Override
  public void add(Entry<E> entry) {
    add(entry.getIndex(), entry.getElement());
  }

  @Override
  public Iterator<Entry<E>> iterator() {
    return Collections.<Entry<E>>unmodifiableList(elements).iterator();
  }

  @Override
  public Stream<? extends Entry<? extends E>> stream() {
    return elements.stream();
  }

  @Override
  public int size() {
    return elements.size();
  }

  private class MutableEntry implements Entry<E> {

    private long index;
    private E element;

    private MutableEntry(long index, E element) {
      this.index = index;
      this.element = element;
    }

    @Override
    public long getIndex() {
      return index;
    }

    @Override
    public E getElement() {
      return element;
    }

  }

  private static final long serialVersionUID = 1;

}
