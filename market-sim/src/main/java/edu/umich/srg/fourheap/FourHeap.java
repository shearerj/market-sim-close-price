package edu.umich.srg.fourheap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static edu.umich.srg.fourheap.OrderType.BUY;
import static edu.umich.srg.fourheap.OrderType.SELL;

import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;

import edu.umich.srg.collect.SetViews;
import edu.umich.srg.util.Optionals;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class provides an efficient order matching mechanism while also producing valid price quotes
 * in constant time. Unless noted, everything is constant time. `n` is the number of order objects,
 * not the quantity of objects i.e. adding more orders at the same price doesn't increase the
 * complexity.
 */

public class FourHeap<P, O extends IOrder<P>> extends AbstractCollection<O> implements Multiset<O> {

  // Orders for unmatched priority where lower is better
  private final Ordering<Key> buyPrice;
  private final Ordering<Key> buyKey;
  private final Ordering<Key> sellPrice;
  private final Ordering<Key> sellKey;
  private final OrderQueue buyMatched;
  private final OrderQueue buyUnmatched;
  private final OrderQueue sellMatched;
  private final OrderQueue sellUnmatched;
  private final Selector<O> selector;

  private long time = Long.MIN_VALUE;

  private FourHeap(Selector<O> selector, Comparator<? super P> priceOrder) {
    this.selector = selector;
    Ordering<Key> keyTime = Ordering.natural().onResultOf(k -> k.time);
    this.sellPrice = Ordering.from(priceOrder).onResultOf(k -> k.price);
    this.sellKey = sellPrice.compound(keyTime);
    this.buyPrice = sellPrice.reverse();
    this.buyKey = buyPrice.compound(keyTime);

    this.buyUnmatched = new OrderQueue(buyKey);
    this.buyMatched = new OrderQueue(buyKey.reverse());
    this.sellUnmatched = new OrderQueue(sellKey);
    this.sellMatched = new OrderQueue(sellKey.reverse());
  }

  public static <P, O extends IOrder<P>> FourHeap<P, O> create(Selector<O> selector,
      Comparator<? super P> priceOrder) {
    return new FourHeap<>(selector, priceOrder);
  }

  public static <P extends Comparable<? super P>, O extends IOrder<P>> FourHeap<P, O> create(
      Selector<O> selector) {
    return new FourHeap<>(selector, Ordering.natural());
  }

  @Override
  public boolean add(O order) {
    add(order, 1);
    return true;
  }

  /** Inserts and returns an order into the fourheap. Complexity: O(log n). */
  @Override
  public int add(O order, int quantity) {
    checkArgument(quantity > 0, "Orders must have positive quantity");
    if (checkNotNull(order.getType()) == BUY) { // buy order
      return add(order, quantity, buyUnmatched, buyMatched, sellUnmatched, sellMatched, buyPrice,
          buyKey);
    } else { // sell order
      return add(order, quantity, sellUnmatched, sellMatched, buyUnmatched, buyMatched, sellPrice,
          sellKey);
    }
  }

  /**
   * Submit order to appropriate queues. This alleviates the need to handle buys and sells
   * differently.
   * 
   * @param order The order to submit
   * @param quantity The number of that order to submit
   * @param ordUnmatched The unmatched queue for that order
   * @param ordMatched The matched queue for that order
   * @param oppUnmatched The unmatched queue for the opposite of that order
   * @param oppMatched The matched queue for the opposite side of that order
   * @param pord A comparator that compares keys only by prices indicating that an order is "better"
   *        if it compares smaller. In this case comparing keys from different order types indicate
   *        keys that could transact if the order is <= 0
   * @param kord A comparator that compares keys by price and time indicating that an order is
   *        better than an order of the same type if it compares smaller
   */
  private int add(O order, int quantity, OrderQueue ordUnmatched, OrderQueue ordMatched,
      OrderQueue oppUnmatched, OrderQueue oppMatched, Comparator<Key> pord, Comparator<Key> kord) {
    Key key = new Key(order.getPrice());
    int countBefore;
    // Order is at least as good as worst matched order
    if (!ordMatched.isEmpty() && kord.compare(key, ordMatched.peekKey().get()) <= 0
        // Order can match with unmatched opps and there's enough quantity
        || !oppUnmatched.isEmpty() && pord.compare(key, oppUnmatched.peekKey().get()) <= 0
            && oppMatched.size() == ordMatched.size()
        // Order is compatible with matched opps, and there quantity to match with
        || !oppMatched.isEmpty() && pord.compare(key, oppMatched.peekKey().get()) <= 0
            && oppMatched.size() > ordMatched.size()) {

      countBefore = ordMatched.add(order, quantity);

      // Fix Order books
      while (ordMatched.size() > oppMatched.size()) {
        if (!oppUnmatched.isEmpty()
            && pord.compare(ordMatched.peekKey().get(), oppUnmatched.peekKey().get()) <= 0) {
          oppUnmatched.pushTo(oppMatched);
        } else if (ordMatched.size() - ordMatched.peekSize() >= oppMatched.size()) {
          ordMatched.pushTo(ordUnmatched);
        } else {
          break;
        }
      }

    } else {
      countBefore = ordUnmatched.add(order, quantity);
    }
    assert invariantsHold();
    return countBefore;
  }

  @Override
  public boolean remove(Object order) {
    return remove(order, 1) > 0;
  }

  /**
   * Withdraws a specific quantity from an order in the fourheap. Behavior is undefined if the order
   * isn't already in the fourheap. Complexity: O(log n).
   */
  @SuppressWarnings("unchecked")
  @Override
  public int remove(Object order, int quantity) {
    checkArgument(quantity >= 0, "Quantity must be nonnegative");
    if (order == null || !(order instanceof IOrder<?>)) {
      return 0;
    } else if (quantity == 0) {
      // Do nothing
      return this.count(order);
    } else if (checkNotNull(((O) order).getType()) == BUY) { // buy order
      return withdraw((O) order, quantity, buyUnmatched, buyMatched, sellUnmatched, sellMatched,
          buyPrice);
    } else { // sell order
      return withdraw((O) order, quantity, sellUnmatched, sellMatched, buyUnmatched, buyMatched,
          sellPrice);
    }
  }

  private int withdraw(O order, int quantity, OrderQueue ordUnmatched, OrderQueue ordMatched,
      OrderQueue oppUnmatched, OrderQueue oppMatched, Comparator<Key> pord) {
    // Remove from unmatched, easy
    int beforeRemoval = ordUnmatched.remove(order, quantity);
    if (beforeRemoval == 0) { // Order wasn't in unmatched
      beforeRemoval = ordMatched.remove(order, quantity);

      // Fix order books
      while (oppMatched.size() > ordMatched.size()) {
        if (!ordUnmatched.isEmpty()
            && pord.compare(ordUnmatched.peekKey().get(), oppMatched.peekKey().get()) <= 0) {
          ordUnmatched.pushTo(ordMatched);
        } else if (oppMatched.size() - oppMatched.peekSize() >= ordMatched.size()) {
          oppMatched.pushTo(oppUnmatched);
        } else {
          break;
        }
      }
    }
    assert invariantsHold();
    return beforeRemoval;
  }

  @Override
  public boolean removeAll(Collection<?> collect) {
    return collect.stream().mapToInt(o -> this.remove(o) ? 1 : 0).sum() > 0;
  }

  /**
   * Clears matching orders from the fourheap, and returns a List of MatchedOrders, which contains
   * the two matched orders, and the quantity matched by that order. Complexity: O(m) where m is the
   * number of matched orders.
   */
  public Collection<MatchedOrders<P, O>> marketClear() {
    // Incriment time every clear
    ++time;

    if (buyMatched.isEmpty()) {
      // If one is empty, the other should be, so we don't check
      return Collections.emptyList();
    }

    int diff = buyMatched.size() - sellMatched.size();
    Map.Entry<Key, Multiset<O>> buy = buyMatched.pollEntry();
    Map.Entry<Key, Multiset<O>> sell = sellMatched.pollEntry();
    if (diff > 0) {
      Multiset<O> unmatched = selector.select(buy.getValue(), diff);
      buyUnmatched.offer(buy.getKey(), unmatched);
    } else if (diff < 0) {
      Multiset<O> unmatched = selector.select(sell.getValue(), -diff);
      sellUnmatched.offer(sell.getKey(), unmatched);
    }

    Iterator<Multiset.Entry<O>> buys =
        Iterables.concat(buy.getValue().entrySet(), buyMatched.entrySet()).iterator();
    Iterator<Multiset.Entry<O>> sells =
        Iterables.concat(sell.getValue().entrySet(), sellMatched.entrySet()).iterator();

    O buyOrder = null;
    O sellOrder = null;
    int buyQuantity = 0;
    int sellQuantity = 0;

    Builder<MatchedOrders<P, O>> transactions = ImmutableList.builder();
    while (buys.hasNext() || sells.hasNext()) {
      if (buyQuantity == 0) {
        Multiset.Entry<O> entry = buys.next();
        buyOrder = entry.getElement();
        buyQuantity = entry.getCount();
      }
      if (sellQuantity == 0) {
        Multiset.Entry<O> entry = sells.next();
        sellOrder = entry.getElement();
        sellQuantity = entry.getCount();
      }

      int quantity = Math.min(buyQuantity, sellQuantity);
      buyQuantity -= quantity;
      sellQuantity -= quantity;
      transactions.add(new MatchedOrders<P, O>(buyOrder, sellOrder, quantity));
    }

    buyMatched.clear();
    sellMatched.clear();
    assert invariantsHold();
    return transactions.build();
  }

  /**
   * Returns the bid quote for the fourheap. A sell order with a price below this is guaranteed to
   * get matched.
   */
  public Optional<P> getBidQuote() {
    Optional<Key> sin = sellMatched.peekKey();
    Optional<Key> bout1 = buyUnmatched.peekKey();
    Optional<Key> bout2 =
        buyMatched.size() > sellMatched.size() ? buyMatched.peekKey() : Optional.empty();
    return Stream.of(sin, bout1, bout2).filter(Optional::isPresent).map(Optional::get).min(buyPrice)
        .map(k -> k.price);
  }

  /**
   * Returns the ask quote for the fourheap. A buy order with a price above this is guaranteed to
   * get matched.
   */
  public Optional<P> getAskQuote() {
    Optional<Key> bin = buyMatched.peekKey();
    Optional<Key> sout1 = sellUnmatched.peekKey();
    Optional<Key> sout2 =
        sellMatched.size() > buyMatched.size() ? sellMatched.peekKey() : Optional.empty();
    return Stream.of(bin, sout1, sout2).filter(Optional::isPresent).map(Optional::get)
        .min(sellPrice).map(k -> k.price);
  }

  private Stream<OrderQueue> queueStream() {
    return Stream.of(buyUnmatched, buyMatched, sellUnmatched, sellMatched);
  }

  @Override
  public boolean contains(Object order) {
    return queueStream().anyMatch(q -> q.contains(order));
  }

  @Override
  public int count(Object order) {
    return queueStream().mapToInt(q -> q.count(order)).sum();
  }

  /** Removes all orders from the fourheap. Does not perform a `marketClear`. */
  @Override
  public void clear() {
    queueStream().forEach(Collection::clear);
  }

  @Override
  public Stream<O> stream() {
    return queueStream().flatMap(Collection::stream);
  }

  @Override
  public Iterator<O> iterator() {
    return stream().iterator();
  }

  /*
   * There's no reason this couldn't be implemented but it wouldn't be used, and would be difficult
   * to write.
   */
  @Override
  public int setCount(O element, int count) {
    throw new UnsupportedOperationException();
  }

  /*
   * There's no reason this couldn't be implemented but it wouldn't be used, and would be difficult
   * to write.
   */
  @Override
  public boolean setCount(O element, int oldCount, int newCount) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<O> elementSet() {
    return SetViews
        .distinctUnion(queueStream().map(Multiset::elementSet).collect(Collectors.toList()));
  }

  @Override
  public Set<Entry<O>> entrySet() {
    return SetViews
        .distinctUnion(queueStream().map(Multiset::entrySet).collect(Collectors.toList()));
  }

  @Override
  public boolean isEmpty() {
    return queueStream().allMatch(Collection::isEmpty);
  }

  @Override
  public int size() {
    return queueStream().mapToInt(Collection::size).sum();
  }

  public int getBidDepth() {
    return buyMatched.size() + buyUnmatched.size();
  }

  public int getAskDepth() {
    return sellMatched.size() + sellUnmatched.size();
  }

  private boolean invariantsHold() {
    Optional<Key> bi = buyMatched.peekKey();
    Optional<Key> bo = buyUnmatched.peekKey();
    Optional<Key> si = sellMatched.peekKey();
    Optional<Key> so = sellUnmatched.peekKey();

    // Unmatched buy out-ranked matched buy
    return Optionals.apply(buyKey::compare, bi, bo).map(x -> x <= 0).orElse(true)
        // Unmatched sell out-ranked matched sell
        && Optionals.apply(sellKey::compare, si, so).map(x -> x <= 0).orElse(true)
        // Matched buys and sells were incompatible
        && Optionals.apply(buyPrice::compare, bi, si).map(x -> x <= 0).orElse(true)
        // Unmatched buys and sells were compatible
        && Optionals.apply(buyPrice::compare, bo, so).map(x -> x > 0).orElse(true)
        // Room to match more sells and they were compatible
        && (buyMatched.size() <= sellMatched.size()
            || Optionals.apply(buyPrice::compare, bi, so).map(x -> x > 0).orElse(true))
        // Room to match more buys and they were compatible
        && (sellMatched.size() <= buyMatched.size()
            || Optionals.apply(sellPrice::compare, si, bo).map(x -> x > 0).orElse(true))
        // Too many matched buys
        && (buyMatched.isEmpty() || buyMatched.size() - buyMatched.peekSize() < sellMatched.size())
        // Too many matched sells
        && (sellMatched.isEmpty()
            || sellMatched.size() - sellMatched.peekSize() < buyMatched.size())
        // Bid and ask depth didn't equal size
        && size() == getBidDepth() + getAskDepth()
        // Buys not all buys
        && buyMatched.elementSet().stream().allMatch(e -> e.getType() == BUY)
        // Buys not all buys
        && buyUnmatched.elementSet().stream().allMatch(e -> e.getType() == BUY)
        // Sells not all sells
        && sellMatched.elementSet().stream().allMatch(e -> e.getType() == SELL)
        // Sells not all sells
        && sellUnmatched.elementSet().stream().allMatch(e -> e.getType() == SELL);
  }

  @Override
  public String toString() {
    String bm =
        buyMatched.entrySet().stream().map(Object::toString).collect(Collectors.joining(", "));
    String bu = buyUnmatched.descendingEntrySet().stream().map(Object::toString)
        .collect(Collectors.joining(", "));
    String sm = sellMatched.descendingEntrySet().stream().map(Object::toString)
        .collect(Collectors.joining(", "));
    String su =
        sellUnmatched.entrySet().stream().map(Object::toString).collect(Collectors.joining(", "));
    return String.format("{%s | %s || %s | %s}", bu, bm, sm, su);
  }

  private class Key {
    private final P price;
    private final long time;

    private Key(P price) {
      this.price = price;
      this.time = FourHeap.this.time;
    }

    @Override
    public int hashCode() {
      return Objects.hash(price, time);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj != null && obj instanceof FourHeap<?, ?>.Key) {
        @SuppressWarnings("unchecked")
        Key that = (Key) obj;
        return Objects.equals(this.price, that.price) && this.time == that.time;
      } else {
        return false;
      }

    }

    @Override
    public String toString() {
      return String.format("<%s, %d>", price, time);
    }
  }

  /*
   * TODO Ideally this would be a Multimap since the Key is now an integral piece of this, however,
   * that requires appropriate creation of several delegates to keep track of information that is
   * only necessary as an internal data strcuture, and thus would be a lot of effort for something
   * that's just a nicer abstraction.
   */
  private class OrderQueue extends AbstractCollection<O> implements Multiset<O> {

    private final NavigableMap<Key, Multiset<O>> queue;
    private final Map<O, Key> keyMap;
    private int size;

    private OrderQueue(Comparator<? super Key> comp) {
      this.queue = new TreeMap<>(comp);
      this.keyMap = new HashMap<>();
    }

    public Optional<Key> peekKey() {
      return isEmpty() ? Optional.empty() : Optional.of(queue.firstKey());
    }

    public int peekSize() {
      return queue.firstEntry().getValue().size();
    }

    private Map.Entry<Key, Multiset<O>> pollEntry() {
      Map.Entry<Key, Multiset<O>> result = queue.pollFirstEntry();
      size -= result.getValue().size();
      result.getValue().elementSet().forEach(keyMap::remove);
      return result;
    }

    private void offer(Key key, Multiset<O> orders) {
      checkArgument(
          orders.elementSet().stream().map(o -> keyMap.put(o, key)).allMatch(Objects::isNull),
          "Some orders were already present");
      checkArgument(Objects.isNull(queue.put(key, orders)), "Key already existed in queue");
      size += orders.size();

      // Check validity
      assert orders.entrySet().stream().allMatch(o -> o.getElement().getPrice().equals(key.price));
    }

    public void pushTo(OrderQueue to) {
      Map.Entry<Key, Multiset<O>> entry = pollEntry();
      to.offer(entry.getKey(), entry.getValue());
    }

    @Override
    public boolean add(O order) {
      add(order, 1);
      return true;
    }

    @Override
    public int add(O order, int quantity) {
      Key key = new Key(order.getPrice());
      checkArgument(Objects.isNull(keyMap.put(order, key)), "Order was already in this queue");
      int countBefore = queue.computeIfAbsent(key, k -> HashMultiset.create()).add(order, quantity);
      size += quantity;
      return countBefore;
    }

    @Override
    public boolean remove(Object obj) {
      return remove(obj, 1) > 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public int remove(Object obj, int quantity) {
      if (obj == null || !(obj instanceof IOrder<?>)) {
        return 0;
      }
      O order = (O) obj;
      Key key = keyMap.get(order);
      if (key == null) {
        return 0;
      }
      Multiset<O> set = queue.get(key);
      if (set == null) {
        return 0;
      }
      int result = set.remove(order, quantity);
      if (set.isEmpty()) {
        queue.remove(key);
      }
      if (!set.contains(order)) {
        keyMap.remove(order);
      }
      size -= Math.min(result, quantity);
      return result;
    }

    @Override
    public void clear() {
      queue.clear();
      keyMap.clear();
      size = 0;
    }

    @Override
    public boolean isEmpty() {
      return queue.isEmpty();
    }

    @Override
    public int size() {
      return size;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object obj) {
      if (obj == null || !(obj instanceof IOrder<?>)) {
        return false;
      }
      O order = (O) obj;
      Key key = keyMap.get(order);
      if (key == null) {
        return false;
      }
      Multiset<O> set = queue.get(key);
      if (set == null) {
        return false;
      }
      return set.contains(order);
    }

    @SuppressWarnings("unchecked")
    @Override
    public int count(Object obj) {
      if (obj == null || !(obj instanceof IOrder<?>)) {
        return 0;
      }
      O order = (O) obj;
      Key key = keyMap.get(order);
      if (key == null) {
        return 0;
      }
      Multiset<O> set = queue.get(key);
      if (set == null) {
        return 0;
      }
      return set.count(order);
    }

    @Override
    public Stream<O> stream() {
      return queue.values().stream().flatMap(Collection::stream);
    }

    @Override
    public Iterator<O> iterator() {
      return stream().iterator();
    }

    @Override
    public Set<O> elementSet() {
      return SetViews.distinctUnion(Collections2.transform(queue.values(), Multiset::elementSet));
    }

    @Override
    public Set<Multiset.Entry<O>> entrySet() {
      return SetViews.distinctUnion(Collections2.transform(queue.values(), Multiset::entrySet));
    }

    public Set<Multiset.Entry<O>> descendingEntrySet() {
      return SetViews.distinctUnion(
          Collections2.transform(queue.descendingMap().values(), Multiset::entrySet));
    }

    @Override
    public String toString() {
      return String.format("{%s}",
          stream().map(Object::toString).collect(Collectors.joining(", ")));
    }

    @Override
    public int setCount(O order, int count) {
      Key key = keyMap.getOrDefault(order, new Key(order.getPrice()));
      Multiset<O> atKey = queue.computeIfAbsent(key, k -> HashMultiset.create());
      int countBefore = atKey.setCount(order, count);
      if (atKey.isEmpty()) {
        queue.remove(key);
      }
      if (!atKey.contains(order)) {
        keyMap.remove(order);
      }
      size += count - countBefore;
      return countBefore;
    }

    @Override
    public boolean setCount(O order, int oldCount, int newCount) {
      Key key = keyMap.getOrDefault(order, new Key(order.getPrice()));
      Multiset<O> atKey = queue.computeIfAbsent(key, k -> HashMultiset.create());
      boolean changed = atKey.setCount(order, oldCount, newCount);
      if (changed) {
        size += newCount - oldCount;
        if (atKey.isEmpty()) {
          queue.remove(key);
        }
      }
      if (!atKey.contains(order)) {
        keyMap.remove(order);
      }
      return changed;
    }

  }

}
