package edu.umich.srg.fourheap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static edu.umich.srg.fourheap.OrderType.BUY;
import static edu.umich.srg.fourheap.OrderType.SELL;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;

import edu.umich.srg.util.Optionals;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NavigableMap;
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
public class FourHeap<P, T, O extends Order<P, T>> {

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

  private FourHeap(Selector<O> selector, Comparator<? super P> priceOrder,
      Comparator<? super T> timeOrder) {
    this.selector = selector;
    Ordering<Key> keyTime = Ordering.from(timeOrder).onResultOf(k -> k.time);
    this.sellPrice = Ordering.from(priceOrder).onResultOf(k -> k.price);
    this.sellKey = sellPrice.compound(keyTime);
    this.buyPrice = sellPrice.reverse();
    this.buyKey = buyPrice.compound(keyTime);

    this.buyUnmatched = new OrderQueue(buyKey);
    this.buyMatched = new OrderQueue(buyKey.reverse());
    this.sellUnmatched = new OrderQueue(sellKey);
    this.sellMatched = new OrderQueue(sellKey.reverse());
  }

  public static <P, T, O extends Order<P, T>> FourHeap<P, T, O> create(Selector<O> selector,
      Comparator<? super P> priceOrder, Comparator<? super T> timeOrder) {
    return new FourHeap<>(selector, priceOrder, timeOrder);
  }

  public static <P extends Comparable<? super P>, T extends Comparable<? super T>, //
      O extends Order<P, T>> FourHeap<P, T, O> create(Selector<O> selector) {
    return new FourHeap<>(selector, Ordering.natural(), Ordering.natural());
  }

  /** Inserts and returns an order into the fourheap. Complexity: O(log n). */
  public void submit(O order, int quantity) {
    checkArgument(quantity > 0, "Orders must have positive quantity");
    if (checkNotNull(order.getType()) == BUY) { // buy order
      submit(order, quantity, buyUnmatched, buyMatched, sellUnmatched, sellMatched, buyPrice,
          buyKey);
    } else { // sell order
      submit(order, quantity, sellUnmatched, sellMatched, buyUnmatched, buyMatched, sellPrice,
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
  private void submit(O order, int quantity, OrderQueue ordUnmatched, OrderQueue ordMatched,
      OrderQueue oppUnmatched, OrderQueue oppMatched, Comparator<Key> pord, Comparator<Key> kord) {
    Key key = new Key(order);
    // Order is at least as good as worst matched order
    if (!ordMatched.isEmpty() && kord.compare(key, ordMatched.peekKey().get()) <= 0
        // Order can match with unmatched opps and there's enough quantity
        || !oppUnmatched.isEmpty() && pord.compare(key, oppUnmatched.peekKey().get()) <= 0
            && oppMatched.size() == ordMatched.size()
        // Order is compatible with matched opps, and there quantity to match with
        || !oppMatched.isEmpty() && pord.compare(key, oppMatched.peekKey().get()) <= 0
            && oppMatched.size() > ordMatched.size()) {

      ordMatched.add(order, quantity);

      // Fix Order books
      // Note: Normally these would need to check that !ordMatched.isEmpty(), but that's impossible
      // given that we just decided to match an order.
      // Move opposite sets to matched if there's room and they match
      while (!oppUnmatched.isEmpty()
          && pord.compare(ordMatched.peekKey().get(), oppUnmatched.peekKey().get()) <= 0
          && ordMatched.size() > oppMatched.size()) {
        oppMatched.offer(oppUnmatched.poll());
      }
      // Move order sets to unmatched if there's not enough opps to match
      while (ordMatched.size() - ordMatched.peekSize() >= oppMatched.size()) {
        ordUnmatched.offer(ordMatched.poll());
      }

    } else {
      ordUnmatched.add(order, quantity);
    }
    assert invariantsHold();
  }

  /**
   * Withdraws a specific quantity from an order in the fourheap. Behavior is undefined if the order
   * isn't already in the fourheap. Complexity: O(log n).
   */
  public void withdraw(O order, int quantity) {
    checkArgument(quantity >= 0, "Quantity must be nonnegative");
    if (quantity == 0) {
      // Do nothing
      return;
    } else if (checkNotNull(order.getType()) == BUY) { // buy order
      withdraw(order, quantity, buyUnmatched, buyMatched, sellUnmatched, sellMatched, buyPrice);
    } else { // sell order
      withdraw(order, quantity, sellUnmatched, sellMatched, buyUnmatched, buyMatched, sellPrice);
    }
  }

  private void withdraw(O order, int quantity, OrderQueue ordUnmatched, OrderQueue ordMatched,
      OrderQueue oppUnmatched, OrderQueue oppMatched, Comparator<Key> pord) {
    // Remove from unmatched, easy
    int before = ordUnmatched.remove(order, quantity);
    if (before == 0) { // Order wasn't in unmatched
      before = ordMatched.remove(order, quantity);

      // Fix order books
      // Move ord sets to matched if there's room and they match
      while (!ordUnmatched.isEmpty()
          && pord.compare(ordUnmatched.peekKey().get(), oppMatched.peekKey().get()) <= 0
          && oppMatched.size() > ordMatched.size()) {
        ordMatched.offer(ordUnmatched.poll());
      }
      // Move opp sets to unmatched if there's not enough ords to match
      while (!oppMatched.isEmpty()
          && oppMatched.size() - oppMatched.peekSize() >= ordMatched.size()) {
        oppUnmatched.offer(oppMatched.poll());
      }

    }
  }

  public boolean contains(O order) {
    return sellUnmatched.contains(order) || sellMatched.contains(order)
        || buyUnmatched.contains(order) || buyMatched.contains(order);
  }

  /**
   * Clears matching orders from the fourheap, and returns a List of MatchedOrders, which contains
   * the two matched orders, and the quantity matched by that order. Complexity: O(m) where m is the
   * number of matched orders.
   */
  public Collection<MatchedOrders<P, T, O>> clear() {
    if (buyMatched.isEmpty()) {
      // If one is empty, the other should be, so we don't check
      return Collections.emptyList();
    }

    int diff = buyMatched.size() - sellMatched.size();
    Multiset<O> buy = buyMatched.poll().getValue();
    Multiset<O> sell = sellMatched.poll().getValue();
    if (diff > 0) {
      Key key = new Key(checkNotNull(Iterables.getFirst(buy, null)));
      Multiset<O> unmatched = selector.select(buy, diff);
      buyUnmatched.offer(key, unmatched);
    } else if (diff < 0) {
      Key key = new Key(checkNotNull(Iterables.getFirst(sell, null)));
      Multiset<O> unmatched = selector.select(sell, -diff);
      sellUnmatched.offer(key, unmatched);
    }

    Iterator<Multiset.Entry<O>> buys = Iterables.concat(buy.entrySet(), buyMatched).iterator();
    Iterator<Multiset.Entry<O>> sells = Iterables.concat(sell.entrySet(), sellMatched).iterator();

    O buyOrder = null;
    O sellOrder = null;
    int buyQuantity = 0;
    int sellQuantity = 0;

    Builder<MatchedOrders<P, T, O>> transactions = ImmutableList.builder();
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
      transactions.add(new MatchedOrders<P, T, O>(buyOrder, sellOrder, quantity));
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

  /** The number of orders in the fourheap. */
  public int size() {
    return sellUnmatched.size() + sellMatched.size() + buyUnmatched.size() + buyMatched.size();
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

    assert Optionals.apply(buyKey::compare, bi, bo).map(x -> x <= 0)
        .orElse(true) : "unmatched buy had better ranking";
    assert Optionals.apply(buyPrice::compare, bi, si).map(x -> x <= 0)
        .orElse(true) : "matched buys and sells not compatable";
    assert Optionals.apply(sellKey::compare, si, so).map(x -> x <= 0)
        .orElse(true) : "unmatched sell had better ranking";
    assert buyMatched.size() <= sellMatched.size()
        || Optionals.apply(buyPrice::compare, bi, so).map(x -> x > 0)
            .orElse(true) : "there was room to match more sells and they were compatable";
    assert sellMatched.size() <= buyMatched.size()
        || Optionals.apply(sellPrice::compare, si, bo).map(x -> x > 0)
            .orElse(true) : "there was room to match more buys and they were compatable";
    assert size() == getBidDepth() + getAskDepth() : "bid and ask depth didn't equal size";
    assert buyMatched.stream().allMatch(e -> e.getElement().getType() == BUY);
    assert buyUnmatched.stream().allMatch(e -> e.getElement().getType() == BUY);
    assert sellMatched.stream().allMatch(e -> e.getElement().getType() == SELL);
    assert sellUnmatched.stream().allMatch(e -> e.getElement().getType() == SELL);

    return true; // If we made it this far, they hold
  }

  @Override
  public String toString() {
    String bm = buyMatched.toString();
    String bu = buyUnmatched.toString();
    String sm = sellMatched.toString();
    String su = sellUnmatched.toString();
    return '{' + bu.substring(1, bu.length() - 1) + " | " + bm.substring(1, bm.length() - 1)
        + " || " + sm.substring(1, sm.length() - 1) + " | " + su.substring(1, su.length() - 1)
        + '}';
  }

  private class Key {
    private final P price;
    private final T time;

    private Key(P price, T time) {
      this.price = price;
      this.time = time;
    }

    private Key(Order<P, T> order) {
      this(order.getPrice(), order.getTime());
    }

    @Override
    public String toString() {
      return "<" + price + ", " + time + ">";
    }
  }

  private class OrderQueue implements Iterable<Multiset.Entry<O>> {

    private final NavigableMap<Key, Multiset<O>> queue;
    private int size;

    private OrderQueue(Comparator<? super Key> comp) {
      this.queue = new TreeMap<>(comp);
    }

    public Optional<Key> peekKey() {
      return isEmpty() ? Optional.empty() : Optional.of(queue.firstKey());
    }

    public int peekSize() {
      return queue.firstEntry().getValue().size();
    }

    public Entry<Key, Multiset<O>> poll() {
      Entry<Key, Multiset<O>> result = queue.pollFirstEntry();
      size -= result.getValue().size();
      return result;
    }

    public void offer(Key key, Multiset<O> orders) {
      Multiset<O> old = queue.put(key, orders);
      size += orders.size();
      // This should never happen
      checkArgument(old == null, "Key (%s) cannot exist in queue", key);
    }

    public void offer(Entry<? extends Key, ? extends Multiset<O>> entry) {
      offer(entry.getKey(), entry.getValue());
    }

    public void add(O order, int quantity) {
      Key key = new Key(order);
      queue.computeIfAbsent(key, k -> HashMultiset.create()).add(order, quantity);
      size += quantity;
    }

    /**
     * Remove quantity of an order from the fourheap.
     * 
     * @return The quantity before removal
     */
    public int remove(O order, int quantity) {
      Key key = new Key(order);
      Multiset<O> set = queue.get(key);
      if (set == null) {
        return 0;
      }
      int result = set.remove(order, quantity);
      if (set.isEmpty()) {
        queue.remove(key);
      }
      size -= Math.min(result, quantity);
      return result;
    }

    public void clear() {
      queue.clear();
      size = 0;
    }

    public boolean isEmpty() {
      return queue.isEmpty();
    }

    public int size() {
      return size;
    }

    public boolean contains(O order) {
      Key key = new Key(order);
      return Optional.ofNullable(queue.get(key)).map(ms -> ms.contains(order)).orElse(false);
    }

    @Override
    public Iterator<Multiset.Entry<O>> iterator() {
      return stream().iterator();
    }

    public Stream<Multiset.Entry<O>> stream() {
      return queue.values().stream().map(Multiset::entrySet).flatMap(Set::stream);
    }

    @Override
    public String toString() {
      return '{' + stream().map(Object::toString).collect(Collectors.joining(", ")) + '}';
    }

  }

}
