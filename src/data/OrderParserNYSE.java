package data;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import com.google.common.collect.Lists;

import entity.market.Price;
import event.TimeStamp;
import fourheap.Order.OrderType;


public class OrderParserNYSE implements OrderParser {
	
	public OrderParserNYSE() {
	}
	
	public List<OrderDatum> process(String fileName) throws FileNotFoundException {
		// Creating the List
		List<OrderDatum> orderDatumList = Lists.newArrayList();
		
		// Opening the file
		File inputFile = new File(fileName);
		Scanner scanner = new Scanner(inputFile);
				
		while (scanner.hasNextLine()) {
			Scanner lineScanner = new Scanner(scanner.nextLine());
			lineScanner.useDelimiter(",");
		
			char messageType = lineScanner.next().charAt(0);
			switch (messageType) {
			case 'A':
				orderDatumList.add(parseAddOrder(lineScanner));
			default:
				break;
			}
			lineScanner.close();
		}
		scanner.close();
		
		return orderDatumList;
	}
	
//	public void process(File inputFile) throws FileNotFoundException {
//		Scanner scanner = new Scanner(inputFile);
//
//		while (scanner.hasNextLine()) {
//			Scanner scanner2 = new Scanner(scanner.nextLine());
//			Scanner lineScanner = scanner2.useDelimiter(",");
//			scanner2.close();
//			
//			Collection<OrderDatum> orderDataList = Lists.newArrayList();
//			
//			char messageType = lineScanner.next().charAt(0);
//
//			switch (messageType) {
//			case 'A':
//				orderDataList.add(parseAddOrder(lineScanner));// store this in a structure
//				break;
//			case 'D':
//				orderDataList.add(parseDeleteOrder(lineScanner));
//				break;
//			case 'M':
//				orderDataList.add(parseModifyOrder(lineScanner));
//				break;
//			case 'I':
//				orderDataList.add(parseImbalanceOrder(lineScanner));
//				break;
//			default:
//				break;
//			}
//			lineScanner.close();
//
//		}
//		scanner.close();
//	}

	public OrderDatum parseAddOrder(Scanner lineScanner) {
		String sequenceNum = lineScanner.next();
		String orderReferenceNum = lineScanner.next();
		char exchangeCode = lineScanner.next().charAt(0);
		OrderType type = (lineScanner.next().charAt(0) == 'B') ? BUY : SELL;
		int quantity = lineScanner.nextInt();
		String stockSymbol = lineScanner.next();
		Price price = new Price(lineScanner.nextDouble());
		TimeStamp timestamp = new TimeStamp(lineScanner.nextInt() + 
				lineScanner.nextInt() * 1000);
		char systemCode = lineScanner.next().charAt(0);
		String quoteId = lineScanner.next();

		OrderDatum orderData = new OrderDatum('A', sequenceNum,
				orderReferenceNum, exchangeCode, stockSymbol, timestamp,
				systemCode, quoteId, price, quantity, type);

		return orderData;
	}

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