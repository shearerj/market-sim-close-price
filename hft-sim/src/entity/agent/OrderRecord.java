package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static fourheap.Order.OrderType.BUY;

import com.google.common.base.Optional;

import entity.market.Market.MarketView;
import entity.market.Price;
import event.TimeStamp;
import fourheap.Order.OrderType;

public class OrderRecord {

	private final MarketView submittedMarket;
	private final OrderType buyOrSell;
	private final TimeStamp createdTime;
	private final Price price;
	private int quantity;
	private Optional<TimeStamp> submitTime;
	private Optional<MarketView> actualMarket;
	
	protected OrderRecord(MarketView submittedMarket, TimeStamp createdTime, OrderType buyOrSell, Price price, int quantity) {
		this.submittedMarket = checkNotNull(submittedMarket);
		this.buyOrSell = checkNotNull(buyOrSell);
		this.price = checkNotNull(price);
		this.createdTime = checkNotNull(createdTime);
		this.submitTime = Optional.absent();
		this.actualMarket = Optional.absent();
		this.quantity = quantity;
		checkArgument(quantity > 0, "Can't submit zero quantity");
	}
	
	public static OrderRecord create(MarketView submittedMarket, TimeStamp createdTime, OrderType buyOrSell, Price price, int quantity) {
		return new OrderRecord(submittedMarket, createdTime, buyOrSell, price, quantity);
	}

	public MarketView getCurrentMarket() {
		return actualMarket.or(submittedMarket);
	}

	public OrderType getOrderType() {
		return buyOrSell;
	}

	public Price getPrice() {
		return price;
	}

	public int getQuantity() {
		return quantity;
	}

	public TimeStamp getCreatedTime() {
		return createdTime;
	}

	public Optional<TimeStamp> getSubmitTime() {
		return submitTime;
	}
	
	void updateMarket(MarketView actualMarket) {
		this.actualMarket = Optional.of(actualMarket);
	}
	
	void updateSubmitTime(TimeStamp submitTime) {
		this.submitTime = Optional.of(submitTime);
	}
	
	void removeQuantity(int removedQuantity) {
		this.quantity = Math.max(0, quantity - removedQuantity);
	}
	
	// Unique objects
	@Override
	public final int hashCode() {
		return super.hashCode();
	}

	// Uniqe objects
	@Override
	public final boolean equals(Object obj) {
		return super.equals(obj);
	}

	@Override
	public String toString() {
		return (buyOrSell == BUY ? "Buy" : "Sell") + ' ' + quantity + " @ " + price;
	}
	
}
