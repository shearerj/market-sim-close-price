package data;

import entity.market.Price;
import event.TimeStamp;
import fourheap.Order.OrderType;

public class MockOrderDatum extends OrderDatum {

	private static final long serialVersionUID = 1L;

	public MockOrderDatum(char messageType, String sequenceNum,
			String orderReferenceNum, char exchangeCode, String stockSymbol,
			TimeStamp timestamp, char systemCode, String quoteId, Price price,
			int quantity, OrderType type) {
		super(messageType, sequenceNum, orderReferenceNum, exchangeCode, stockSymbol,
				timestamp, systemCode, quoteId, price, quantity, type);
	}

	public MockOrderDatum(TimeStamp timestamp, Price price, int quantity, OrderType type) {
		this(' ', "", "", ' ', "MOCK", timestamp, ' ', "", price, quantity, type);
	}
}
