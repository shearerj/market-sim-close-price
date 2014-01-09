package data;

import entity.market.Price;
import event.TimeStamp;

public class MockOrderDatum extends OrderDatum {

	private static final long serialVersionUID = 1L;

	public MockOrderDatum(char messageType, String sequenceNum,
			String orderReferenceNum, char exchangeCode, String stockSymbol,
			TimeStamp timestamp, char systemCode, String quoteId, Price price,
			int quantity, boolean isBuy) {
		super(messageType, sequenceNum, orderReferenceNum, exchangeCode, stockSymbol,
				timestamp, systemCode, quoteId, price, quantity, isBuy);
	}

	public MockOrderDatum(TimeStamp timestamp, Price price, int quantity, boolean isBuy) {
		this(' ', "", "", ' ', "MOCK", timestamp, ' ', "", price, quantity, isBuy);
	}
}
