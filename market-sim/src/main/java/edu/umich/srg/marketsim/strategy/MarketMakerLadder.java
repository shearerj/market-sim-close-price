package edu.umich.srg.marketsim.strategy;

import static com.google.common.base.Preconditions.checkArgument;
import static edu.umich.srg.fourheap.Order.OrderType.BUY;
import static edu.umich.srg.fourheap.Order.OrderType.SELL;

import edu.umich.srg.marketsim.Price;

import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class MarketMakerLadder {

  private final int stepSize, numRungs, offset;

  public MarketMakerLadder(int rungSeperation, int numRungs, boolean tickImprovement,
      boolean tickOutside) {
    checkArgument(rungSeperation > 0);
    checkArgument(numRungs > 0);
    this.stepSize = rungSeperation;
    this.numRungs = numRungs;
    this.offset = tickImprovement ? (tickOutside ? 1 : -1) : 0;
  }

  public Stream<OrderDesc> createLadder(Price highestSell, Price lowestBuy) {
    Stream<OrderDesc> buys = toStream(lowestBuy.longValue(), -highestSell.longValue())
        .mapToObj(p -> OrderDesc.of(BUY, Price.of(p)));
    Stream<OrderDesc> sells = toStream(-highestSell.longValue(), lowestBuy.longValue()).map(p -> -p)
        .mapToObj(p -> OrderDesc.of(SELL, Price.of(p)));
    return Stream.concat(buys, sells);
  }

  private LongStream toStream(long init, long cross) {
    return IntStream.range(0, numRungs).mapToLong(s -> init + offset + this.stepSize * s)
        .filter(p -> p > -(cross + offset));
  }

}
