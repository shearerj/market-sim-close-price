package edu.umich.srg.fourheap;

import static edu.umich.srg.fourheap.OrderType.BUY;
import static edu.umich.srg.fourheap.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;

import edu.umich.srg.testing.Repeat;
import edu.umich.srg.testing.RepeatRule;
import edu.umich.srg.testing.TestInts;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.Random;

@RunWith(Theories.class)
public class FourHeapTest {


  @Rule
  public RepeatRule repeatRule = new RepeatRule();

  private final static Random rand = new Random();

  private FourHeap<Integer, Ord> fh;

  @Before
  public void setup() {
    fh = FourHeap.create(PrioritySelector.create(Ordering.arbitrary()));
  }

  @Test
  public void insertOneBuyTest() {
    fh.add(new Ord(BUY, 5), 3);

    assertEquals(5, (int) fh.getBidQuote().get());
    assertFalse(fh.getAskQuote().isPresent());
    assertEquals(3, fh.size());
    assertEquals(3, fh.getBidDepth());
    assertEquals(0, fh.getAskDepth());
  }

  @Test
  public void insertOneSellTest() {
    fh.add(new Ord(SELL, 5), 3);

    assertFalse(fh.getBidQuote().isPresent());
    assertEquals(5, (int) fh.getAskQuote().get());
    assertEquals(3, fh.size());
    assertEquals(0, fh.getBidDepth());
    assertEquals(3, fh.getAskDepth());
  }


  @Test
  public void matchTest1() {
    fh.add(new Ord(BUY, 7), 3);
    fh.add(new Ord(SELL, 5), 3);

    assertEquals(5, (int) fh.getBidQuote().get());
    assertEquals(7, (int) fh.getAskQuote().get());
    assertEquals(6, fh.size());
    assertEquals(3, fh.getBidDepth());
    assertEquals(3, fh.getAskDepth());
  }

  @Test
  public void matchTest2() {
    fh.add(new Ord(BUY, 7), 3);
    fh.add(new Ord(SELL, 5), 5);

    assertEquals(5, (int) fh.getBidQuote().get());
    assertEquals(5, (int) fh.getAskQuote().get());
    assertEquals(8, fh.size());
    assertEquals(3, fh.getBidDepth());
    assertEquals(5, fh.getAskDepth());
  }

  @Test
  public void matchTest3() {
    fh.add(new Ord(BUY, 7), 5);
    fh.add(new Ord(SELL, 5), 3);

    assertEquals(7, (int) fh.getBidQuote().get());
    assertEquals(7, (int) fh.getAskQuote().get());
    assertEquals(8, fh.size());
    assertEquals(5, fh.getBidDepth());
    assertEquals(3, fh.getAskDepth());
  }

  @Test
  public void oustTest() {
    fh.add(new Ord(BUY, 3), 2);
    fh.add(new Ord(BUY, 5), 1);
    fh.add(new Ord(BUY, 6), 1);
    fh.add(new Ord(BUY, 7), 1);

    fh.add(new Ord(SELL, 4), 1);
    fh.add(new Ord(SELL, 2), 2);

    fh.add(new Ord(SELL, 1), 2);
  }

  @Test
  public void nomatchTest1() {
    fh.add(new Ord(SELL, 3), 1);
    fh.add(new Ord(BUY, 10), 1);
    fh.add(new Ord(BUY, 5), 1); // Shouldn't match

    assertEquals(10, (int) fh.getAskQuote().get());
    assertEquals(5, (int) fh.getBidQuote().get());
  }

  @Test
  public void nomatchTest3() {
    fh.add(new Ord(SELL, 3), 1);
    fh.add(new Ord(BUY, 10), 1); // These match
    fh.add(new Ord(SELL, 5), 1); // This doesn't because there's not enough quantity
    fh.add(new Ord(BUY, 5), 1); // This would match with

    assertEquals(2, fh.marketClear().size());
  }

  @Test
  public void insertMatchedTest1() {
    fh.add(new Ord(SELL, 5), 3);
    fh.add(new Ord(BUY, 7), 3);
    fh.add(new Ord(BUY, 4), 1);

    assertEquals(5, (int) fh.getBidQuote().get());
    assertEquals(7, (int) fh.getAskQuote().get());
    assertEquals(7, fh.size());
    assertEquals(4, fh.getBidDepth());
    assertEquals(3, fh.getAskDepth());
  }

  @Test
  public void insertMatchedTest2() {
    fh.add(new Ord(SELL, 5), 3);
    fh.add(new Ord(BUY, 7), 3);
    fh.add(new Ord(BUY, 6), 1);

    assertEquals(6, (int) fh.getBidQuote().get());
    assertEquals(7, (int) fh.getAskQuote().get());
    assertEquals(7, fh.size());
    assertEquals(4, fh.getBidDepth());
    assertEquals(3, fh.getAskDepth());
  }

  @Test
  public void insertMatchedTest3() {
    fh.add(new Ord(SELL, 5), 3);
    fh.add(new Ord(BUY, 7), 3);
    fh.add(new Ord(BUY, 8), 1);

    assertEquals(7, (int) fh.getBidQuote().get());
    assertEquals(7, (int) fh.getAskQuote().get());
    assertEquals(7, fh.size());
    assertEquals(4, fh.getBidDepth());
    assertEquals(3, fh.getAskDepth());
  }

  @Test
  public void withdrawOneBuyTest() {
    Ord o = new Ord(BUY, 5);
    fh.add(o, 3);
    fh.remove(o, 2);

    assertTrue(fh.contains(o));
    assertEquals(5, (int) fh.getBidQuote().get());
    assertFalse(fh.getAskQuote().isPresent());
    assertEquals(1, fh.size());
    assertEquals(1, fh.getBidDepth());
    assertEquals(0, fh.getAskDepth());

    fh.remove(o, 1);

    assertFalse(fh.contains(o));
    assertFalse(fh.getBidQuote().isPresent());
    assertFalse(fh.getAskQuote().isPresent());
    assertEquals(0, fh.size());
    assertEquals(0, fh.getBidDepth());
    assertEquals(0, fh.getAskDepth());
  }


  @Test
  public void withdrawOneSellTest() {
    Ord o = new Ord(SELL, 5);
    fh.add(o, 3);
    fh.remove(o, 2);

    assertTrue(fh.contains(o));
    assertFalse(fh.getBidQuote().isPresent());
    assertEquals(5, (int) fh.getAskQuote().get());
    assertEquals(1, fh.size());
    assertEquals(0, fh.getBidDepth());
    assertEquals(1, fh.getAskDepth());

    fh.remove(o, 1);

    assertFalse(fh.contains(o));
    assertFalse(fh.getBidQuote().isPresent());
    assertFalse(fh.getAskQuote().isPresent());
    assertEquals(0, fh.size());
    assertEquals(0, fh.getBidDepth());
    assertEquals(0, fh.getAskDepth());
  }

  @Test
  public void withdrawMatchTest1() {
    Ord os = new Ord(SELL, 5);
    Ord ob = new Ord(BUY, 7);
    fh.add(os, 3);
    fh.add(ob, 3);
    fh.remove(ob, 2);

    assertEquals(5, (int) fh.getBidQuote().get());
    assertEquals(5, (int) fh.getAskQuote().get());
    assertEquals(4, fh.size());
    assertEquals(1, fh.getBidDepth());
    assertEquals(3, fh.getAskDepth());

    fh.remove(os, 3);

    assertEquals(7, (int) fh.getBidQuote().get());
    assertFalse(fh.getAskQuote().isPresent());
    assertEquals(1, fh.size());
    assertEquals(1, fh.getBidDepth());
    assertEquals(0, fh.getAskDepth());
  }

  @Test
  public void withdrawMatchTest2() {
    Ord ob = new Ord(BUY, 7);
    Ord os = new Ord(SELL, 5);
    fh.add(ob, 3);
    fh.add(os, 5);
    fh.remove(os, 3);

    assertEquals(7, (int) fh.getBidQuote().get());
    assertEquals(7, (int) fh.getAskQuote().get());
    assertEquals(5, fh.size());
    assertEquals(3, fh.getBidDepth());
    assertEquals(2, fh.getAskDepth());

    fh.remove(ob, 3);

    assertFalse(fh.getBidQuote().isPresent());
    assertEquals(5, (int) fh.getAskQuote().get());
    assertEquals(2, fh.size());
    assertEquals(0, fh.getBidDepth());
    assertEquals(2, fh.getAskDepth());
  }

  @Test
  public void withdrawMatchTest3() {
    fh.add(new Ord(SELL, 5), 3);
    Ord ob = new Ord(BUY, 7);
    fh.add(ob, 5);
    fh.remove(ob, 4);

    assertEquals(5, (int) fh.getBidQuote().get());
    assertEquals(5, (int) fh.getAskQuote().get());
    assertEquals(4, fh.size());
    assertEquals(1, fh.getBidDepth());
    assertEquals(3, fh.getAskDepth());
  }

  /** Test that withdrawing with orders waiting to get matched actually works appropriately */
  @Test
  public void withdrawWithWaitingOrders1() {
    Ord o = new Ord(BUY, 4);
    fh.add(o, 3);
    fh.add(new Ord(SELL, 1), 3);
    fh.add(new Ord(SELL, 2), 2);
    fh.add(new Ord(BUY, 3), 4);
    fh.remove(o, 1);
    fh.marketClear();
  }

  @Test
  public void withdrawWithWaitingOrders2() {
    Ord o = new Ord(SELL, 1);
    fh.add(o, 3);
    fh.add(new Ord(BUY, 4), 3);
    fh.add(new Ord(BUY, 3), 2);
    fh.add(new Ord(SELL, 2), 4);
    fh.remove(o, 3);
    fh.marketClear();
  }

  /** Test a strange edge case with withdrawing orders, where quantity may get misinterpreted. */
  @Test
  public void strangeWithdrawEdgeCase1() {
    fh.add(new Ord(BUY, 4), 3);
    Ord o = new Ord(SELL, 1);
    fh.add(o, 3);
    fh.add(new Ord(SELL, 2), 2);
    fh.add(new Ord(BUY, 3), 4);
    fh.remove(o, 3);
    fh.marketClear();
  }

  @Test
  public void strangeWithdrawEdgeCase2() {
    fh.add(new Ord(SELL, 1), 3);
    Ord o = new Ord(BUY, 4);
    fh.add(o, 3);
    fh.add(new Ord(BUY, 3), 2);
    fh.add(new Ord(SELL, 2), 4);
    fh.remove(o, 3);
    fh.marketClear();
  }

  @Test
  public void withdrawTest3() {
    fh.add(new Ord(SELL, 3), 3);
    Ord o = new Ord(BUY, 10);
    fh.add(o, 3);
    fh.add(new Ord(BUY, 5), 2);

    assertEquals(10, (int) fh.getAskQuote().get());
    assertEquals(5, (int) fh.getBidQuote().get());

    fh.remove(o, 1);

    assertEquals(5, (int) fh.getAskQuote().get());
    assertEquals(5, (int) fh.getBidQuote().get());
  }

  @Test
  public void withdrawTest4() {
    fh.add(new Ord(SELL, 3), 1);
    fh.add(new Ord(SELL, 4), 1);
    fh.add(new Ord(BUY, 5), 1);
    Ord o = new Ord(BUY, 4);
    fh.add(o, 1);
    fh.remove(o, 1);

    assertEquals(4, (int) fh.getAskQuote().get());
    assertEquals(3, (int) fh.getBidQuote().get());
  }

  @Test
  public void withdrawTest5() {
    fh.add(new Ord(SELL, 3), 2);
    fh.add(new Ord(BUY, 2), 1);
    Ord o = new Ord(BUY, 4);
    fh.add(o, 1);
    fh.remove(o, 1);

    assertEquals(3, (int) fh.getAskQuote().get());
    assertEquals(2, (int) fh.getBidQuote().get());
  }

  @Test
  public void emptyClearTest() {
    fh.add(new Ord(SELL, 7), 3);
    fh.add(new Ord(BUY, 5), 3);
    assertTrue(fh.marketClear().isEmpty());
  }

  @Test
  public void clearTest() {
    Ord os = new Ord(SELL, 5);
    Ord ob = new Ord(BUY, 7);

    fh.add(os, 2);
    fh.add(ob, 3);
    Collection<MatchedOrders<Integer, Ord>> transactions = fh.marketClear();

    assertEquals(1, transactions.size());
    MatchedOrders<Integer, Ord> trans = Iterables.getOnlyElement(transactions);
    assertEquals(os, trans.getSell());
    assertEquals(ob, trans.getBuy());
    assertEquals(2, trans.getQuantity());
    assertTrue(fh.contains(ob));
    assertEquals(1, fh.size());
    assertEquals(1, fh.getBidDepth());
    assertEquals(0, fh.getAskDepth());
    assertFalse(fh.contains(os));
    assertTrue(fh.contains(ob));
  }

  @Test
  public void multiOrderClearTest() {
    Ord os = new Ord(SELL, 5);
    fh.add(os, 3);
    fh.add(new Ord(SELL, 6), 2);
    Ord ob = new Ord(BUY, 7);
    fh.add(ob, 4);
    Collection<MatchedOrders<Integer, Ord>> transactions = fh.marketClear();

    assertEquals(2, transactions.size());
    assertEquals(1, fh.size());
    assertEquals(0, fh.getBidDepth());
    assertEquals(1, fh.getAskDepth());
    assertFalse(fh.contains(os));
    assertFalse(fh.contains(ob));

    boolean one = false, three = false;
    for (MatchedOrders<Integer, Ord> trans : transactions) {
      switch (trans.getQuantity()) {
        case 1:
          assertEquals(ob, trans.getBuy());
          assertNotEquals(os, trans.getSell());
          one = true;
          break;
        case 3:
          assertEquals(os, trans.getSell());
          assertEquals(ob, trans.getBuy());
          three = true;
          break;
        default:
          fail("Incorrect transaction quantities");
      }
    }
    assertTrue(one);
    assertTrue(three);
  }

  @Test
  public void containsTest() {
    Ord ob = new Ord(BUY, 5);
    fh.add(ob, 1);
    assertTrue(fh.contains(ob));
    Ord os = new Ord(SELL, 6);
    fh.add(os, 1);
    assertTrue(fh.contains(os));
    os = new Ord(SELL, 4);
    fh.add(os, 1); // Verify that sell order @ 4 which has matched is still in FH
    assertTrue(fh.contains(os));
    assertTrue(fh.contains(ob));
  }

  @Test
  public void matchedQuoteTest() {
    // Test when only matched orders in order book
    fh.add(new Ord(BUY, 10), 1);
    fh.add(new Ord(SELL, 5), 1);
    // BID=max{max(matched sells), max(unmatched buys)}
    // ASK=min{min(matched buys), min(unmatched sells)}
    assertEquals(5, (int) fh.getBidQuote().get());
    assertEquals(10, (int) fh.getAskQuote().get());
  }

  @Test
  public void askQuoteTest() {
    assertFalse(fh.getBidQuote().isPresent());
    assertFalse(fh.getAskQuote().isPresent());

    // Test when no matched orders
    fh.add(new Ord(SELL, 10), 1);
    assertEquals(10, (int) fh.getAskQuote().get());
    assertFalse(fh.getBidQuote().isPresent());
    fh.add(new Ord(BUY, 5), 1);
    assertEquals(5, (int) fh.getBidQuote().get());

    // Test when some orders matched
    // BID=max{max(matched sells), max(unmatched buys)} -> max(10, 5)
    // ASK=min{min(matched buys), min(unmatched sells)} -> min(15, -)
    fh.add(new Ord(BUY, 15), 1);
    assertEquals(15, (int) fh.getAskQuote().get()); // the matched buy at 15
    assertEquals(10, (int) fh.getBidQuote().get());

    // Now orders in each container in FH
    fh.add(new Ord(SELL, 20), 1);
    assertEquals(10, (int) fh.getBidQuote().get()); // max(10, 5)
    assertEquals(15, (int) fh.getAskQuote().get()); // min(15, 20)
  }

  @Test
  public void bidQuoteTest() {
    assertFalse(fh.getBidQuote().isPresent());
    assertFalse(fh.getAskQuote().isPresent());

    // Test when no matched orders
    fh.add(new Ord(BUY, 15), 1);
    assertEquals(15, (int) fh.getBidQuote().get());
    assertFalse(fh.getAskQuote().isPresent());
    fh.add(new Ord(SELL, 20), 1);
    assertEquals(20, (int) fh.getAskQuote().get());

    // Test when some orders matched
    // BID=max{max(matched sells), max(unmatched buys)} -> max(10, -)
    // ASK=min{min(matched buys), min(unmatched sells)} -> min(15, 20)
    fh.add(new Ord(SELL, 10), 1);
    assertEquals(10, (int) fh.getBidQuote().get()); // the matched sell at 10
    assertEquals(15, (int) fh.getAskQuote().get());

    // Now orders in each container in FH
    fh.add(new Ord(BUY, 5), 1);
    assertEquals(10, (int) fh.getBidQuote().get()); // max(10, 5)
    assertEquals(15, (int) fh.getAskQuote().get()); // min(15, 20)
  }

  @Test
  public void buyTimePriorityTest() {
    Ord buy1 = new Ord(BUY, 2);
    Ord buy2 = new Ord(BUY, 2);
    fh.add(buy1, 1);
    assertTrue(fh.marketClear().isEmpty());
    fh.add(buy2);
    fh.add(new Ord(SELL, 1), 1);
    Iterable<MatchedOrders<Integer, Ord>> matches = fh.marketClear();

    assertEquals(1, Iterables.size(matches));
    MatchedOrders<Integer, Ord> match = matches.iterator().next();
    assertEquals(buy1, match.getBuy());
    assertEquals(2, (int) fh.getBidQuote().get());
    assertFalse(fh.getAskQuote().isPresent());
    assertFalse(fh.contains(buy1));
    assertTrue(fh.contains(buy2));
  }

  @Test
  public void sellTimePriorityTest() {
    Ord sell1 = new Ord(SELL, 1);
    Ord sell2 = new Ord(SELL, 1);
    fh.add(sell1, 1);
    assertTrue(fh.marketClear().isEmpty());
    fh.add(sell2);
    fh.add(new Ord(BUY, 2), 1);
    Iterable<MatchedOrders<Integer, Ord>> matches = fh.marketClear();

    assertEquals(1, Iterables.size(matches));
    MatchedOrders<Integer, Ord> match = matches.iterator().next();
    assertEquals(sell1, match.getSell());
    assertFalse(fh.getBidQuote().isPresent());
    assertEquals(1, (int) fh.getAskQuote().get());
    assertFalse(fh.contains(sell1));
    assertTrue(fh.contains(sell2));
  }

  @Test
  public void clearQuoteTest() {
    fh.add(new Ord(BUY, 2), 1);
    fh.add(new Ord(SELL, 1), 1);
    fh.marketClear();
    assertFalse(fh.getBidQuote().isPresent());
    assertFalse(fh.getAskQuote().isPresent());
  }

  @Test
  public void specificInvariantTest1() {
    fh.add(new Ord(BUY, 2), 1);
    fh.add(new Ord(SELL, 1), 1);
    fh.add(new Ord(SELL, 4), 1);
    fh.add(new Ord(BUY, 3), 1);
    fh.add(new Ord(BUY, 5), 1);
  }

  @Test
  public void specificInvariantTest2() {
    fh.add(new Ord(SELL, 4), 1);
    fh.add(new Ord(BUY, 5), 1);
    fh.add(new Ord(BUY, 2), 1);
    fh.add(new Ord(SELL, 3), 1);
    fh.add(new Ord(SELL, 1), 1);
  }

  @Theory
  @Repeat(100)
  public void quoteInvariantTest(@TestInts({20}) int time) {
    for (; time > 0; time--) {
      fh.add(new Ord(rand.nextBoolean() ? BUY : SELL, rand.nextInt(20) + 1), 1);
    }
  }

  @Theory
  @Repeat(100)
  public void quoteInvariantWithdrawTest(@TestInts({10}) int num, @TestInts({20}) int time,
      @TestInts({3}) int maxQuant) {
    Ord[] ords = new Ord[num];
    for (; time > 0; time--) {
      int pos = rand.nextInt(num);
      if (ords[pos] != null) {
        fh.remove(ords[pos], maxQuant);
      }
      ords[pos] = new Ord(rand.nextBoolean() ? BUY : SELL, rand.nextInt(20) + 1);
      int quant = rand.nextInt(maxQuant) + 1;
      fh.add(ords[pos], quant);
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void negativeQuantitySubmitTest() {
    fh.add(new Ord(BUY, 3), 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void negativeQuantityWithdrawTest() {
    fh.remove(new Ord(BUY, 3), -1);
  }

  private static class Ord implements IOrder<Integer> {

    private final int price;
    private final OrderType type;

    private Ord(OrderType type, int price) {
      this.price = price;
      this.type = type;
    }

    @Override
    public Integer getPrice() {
      return price;
    }

    @Override
    public OrderType getType() {
      return type;
    }

    @Override
    public String toString() {
      return String.format("(%s @ %s)", type, price);
    }
  }

}
