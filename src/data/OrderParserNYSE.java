package data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import java.util.Collection;

import com.google.common.collect.Lists;

import entity.market.Price;
import event.TimeStamp;
import fourheap.Order.OrderType;
import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;


public class OrderParserNYSE implements OrderParser {
	
	public OrderParserNYSE() {
	}
	
	public List<OrderDatum> process(Path path) throws IOException {
		// Creating the List
		List<OrderDatum> orderDatumList = Lists.newArrayList();
		
		// Opening the file
		Scanner scanner = new Scanner(path);
				
		int x =0;
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			List<String> elements = Lists.newArrayList(line.split(","));
		
			char messageType = elements.get(0).charAt(0);
			switch (messageType) {
			case 'A':
				orderDatumList.add(parseAddOrder(elements));
			default:
				break;
			}
		}
		scanner.close();
		
		return orderDatumList;
	}
	
	public OrderDatum parseAddOrder(List<String> elements) {
		// Error Check: Add order must have 13 columns
//		if(elements.size() != 13) return null;
		
		char messageType = elements.get(0).charAt(0);
		String sequencenumber = elements.get(1);
		String orderReferenceNumber = elements.get(2);
		char exchangeCode = elements.get(3).charAt(0);
		OrderType type = (elements.get(4).charAt(0) == 'B') ? BUY : SELL;
		int quantity = new Integer(elements.get(5));
		String symbol = elements.get(6);
		Price price = new Price(new Double(elements.get(7)));
		int seconds = new Integer(elements.get(8));
		int milliseconds = new Integer(elements.get(9));
		TimeStamp timestamp = new TimeStamp(seconds*1000 + milliseconds);
		char systemCode = elements.get(10).charAt(0);
		String quoteID = elements.get(11);
		
		return new OrderDatum(messageType, sequencenumber, orderReferenceNumber,
				exchangeCode, symbol, timestamp, systemCode, quoteID, price, quantity, 
				type);
	}

//	public OrderDatum parseAddOrder(Scanner lineScanner) {
//		String sequenceNum = lineScanner.next();
//		String orderReferenceNum = lineScanner.next();
//		char exchangeCode = lineScanner.next().charAt(0);
//		OrderType type = (lineScanner.next().charAt(0) == 'B') ? BUY : SELL;
//		int quantity = lineScanner.nextInt();
//		String stockSymbol = lineScanner.next();
//		Price price = new Price(lineScanner.nextDouble());
//		TimeStamp timestamp = new TimeStamp(lineScanner.nextInt() + 
//				lineScanner.nextInt() * 1000);
//		char systemCode = lineScanner.next().charAt(0);
//		String quoteId = lineScanner.next();
//
//		OrderDatum orderData = new OrderDatum('A', sequenceNum,
//				orderReferenceNum, exchangeCode, stockSymbol, timestamp,
//				systemCode, quoteId, price, quantity, type);
//
//		return orderData;
//	}

	public OrderDatum parseDeleteOrder(Scanner lineScanner) {
		String sequenceNum = lineScanner.next();
		String orderReferenceNum = lineScanner.next();
		TimeStamp timestamp = new TimeStamp(lineScanner.nextInt() + 
				lineScanner.nextInt() * 1000);
		String stockSymbol = lineScanner.next();
		char exchangeCode = lineScanner.next().charAt(0);
		char systemCode = lineScanner.next().charAt(0);
		String quoteId = lineScanner.next();
		OrderType type = (lineScanner.next().charAt(0) == 'B') ? BUY : SELL;

		OrderDatum orderData = new OrderDatum('D', sequenceNum,
				orderReferenceNum, exchangeCode, stockSymbol, timestamp,
				systemCode, quoteId, new Price(0), // price doesnt matter since
													// delete
				0, // quantity as well
				type);

		return orderData;

	}

	public OrderDatum parseModifyOrder(Scanner lineScanner) {
		String sequenceNum = lineScanner.next();
		String orderReferenceNum = lineScanner.next();
		int quantity = lineScanner.nextInt();
		Price price = new Price(lineScanner.nextDouble());
		TimeStamp timestamp = new TimeStamp(lineScanner.nextInt() + 
				lineScanner.nextInt() * 1000);
		String stockSymbol = lineScanner.next();
		char exchangeCode = lineScanner.next().charAt(0);
		char systemCode = lineScanner.next().charAt(0);
		String quoteId = lineScanner.next();
		OrderType type = (lineScanner.next().charAt(0) == 'B') ? BUY : SELL;

		OrderDatum orderData = new OrderDatum('M', sequenceNum,
				orderReferenceNum, exchangeCode, stockSymbol, timestamp,
				systemCode, quoteId, price, quantity, type);

		return orderData;

	}

	public OrderDatum parseImbalanceOrder(Scanner lineScanner) {
		String sequenceNum = lineScanner.next();
		String stockSymbol = lineScanner.next();
		Price price = new Price(lineScanner.nextDouble());
		int quantity = lineScanner.nextInt();
		int totalImbalance = lineScanner.nextInt();
		TimeStamp timestamp = new TimeStamp(lineScanner.nextInt() + 
				lineScanner.nextInt() * 1000);
		int marketImbalance = lineScanner.nextInt();
		char auctionType = lineScanner.next().charAt(0);
		int auctionTime = lineScanner.nextInt();
		char exchangeCode = lineScanner.next().charAt(0);
		char systemCode = lineScanner.next().charAt(0);
		String quoteId = "", orderReferenceNum = "";
		OrderType type = BUY; // doesn't matter because not used
		OrderDatum orderData = new OrderDatum('I', sequenceNum,
				orderReferenceNum, exchangeCode, stockSymbol, timestamp,
				systemCode, quoteId, price, quantity, type);
		orderData.setAuctionTime(auctionTime);
		orderData.setTotalImbalance(totalImbalance);
		orderData.setAuctionType(auctionType);
		orderData.setMarketImbalance(marketImbalance);
		return orderData;

	}

	public File readFile(String filename) throws IOException {
		File inputFile = new File(filename).getCanonicalFile();
		return inputFile;
	}


}