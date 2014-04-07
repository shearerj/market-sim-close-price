package data;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;

import entity.market.Price;
import event.TimeStamp;
import fourheap.Order.OrderType;

public class NasdaqParser extends MarketDataParser {
	
	public NasdaqParser(String pathName) throws IOException {
		super(pathName);
	}

	@Override
	public PeekingIterator<OrderDatum> getIterator() {
		// TODO Auto-generated method stub
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			List<String> elements = Lists.newArrayList(line.split(","));
			
			char messageType = elements.get(0).charAt(0);
			switch (messageType) {
			case 'T':
				curSeconds = parseTimeStamp(elements);
				break;
			case 'A':
				orderDatumList.add(parseAddOrder(elements));
				 break;
			case 'D':
				parseDeleteOrder(elements);
				break;
			default:
				break;
			}
		}
		
		return Iterators.peekingIterator(orderDatumList.iterator());
	}

	private int curSeconds = 0;
	private int parseTimeStamp(List<String> elements) {
		return new Integer(elements.get(1));
	}
	
	private OrderDatum parseAddOrder(List<String> elements) {
		int milliseconds = new Integer(elements.get(1)) / 1000000;
		TimeStamp timeStamp = TimeStamp.create(curSeconds + milliseconds);

		int orderRefNumber = new Integer(elements.get(2));
		
		char buyIndicator = elements.get(3).charAt(0);
		OrderType orderType = (buyIndicator == 'B') ? OrderType.BUY : OrderType.SELL;
		
		int quantity = new Integer(elements.get(4));
		Price price = new Price(new Integer(elements.get(6)));
		
		return new OrderDatum(orderRefNumber, timeStamp, price, quantity, orderType);
	}
	
	private void parseDeleteOrder(List<String> elements) {
		int orderRefNum = new Integer(elements.get(2));
		int milliseconds = new Integer(elements.get(1)) / 1000000;
		TimeStamp deleteTime = TimeStamp.create(curSeconds + milliseconds);
		
		// So we can reverse search the orders
		Iterator<OrderDatum> itr = Lists.reverse(orderDatumList).iterator();
		
		while (itr.hasNext()) {
			OrderDatum orderDatum = itr.next();
			
			// Check if the order reference numbers match
			if (orderDatum.getOrderRefNum() == orderRefNum) {
				TimeStamp duration = deleteTime.minus(orderDatum.getTimeStamp());
				orderDatum.setDuration(duration);
				break;
			}
		}
		
	}
}
