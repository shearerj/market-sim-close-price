package edu.umich.srg.fourheap;

import com.google.common.collect.Multiset;

public interface Selector<E> {

  /**
   * Modifies the original multiset by removing quantity elements and returning a new multiset with
   * them. After executing select return.size() + original.size() should equal orignial.size()
   * before invocation.
   */
  Multiset<E> select(Multiset<E> original, int quantity);

}
