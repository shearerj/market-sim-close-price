package data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import entity.market.Price;
import event.TimeStamp;

public class OrderParserNYSE implements OrderParser {
	OrderParserNYSE() {
	}
	
	OrderParserNYSE(String filename) throws IOException {
		File inputFile = readFile(filename);
		process(inputFile);
	}

	public void process(File inputFile) throws FileNotFoundException {
		Scanner scanner = new Scanner(inputFile);

		while (scanner.hasNextLine()) {
			Scanner lineScanner = new Scanner(scanner.nextLine()).useDelimiter(",");

			char messageType = lineScanner.next().charAt(0);

			switch (messageType) {
			case 'A':
				orderDataList.add(parseAddOrder(lineScanner));// store this in a structure
				break;
			case 'D':
				orderDataList.add(parseDeleteOrder(lineScanner));
				break;
			case 'M':
				orderDataList.add(parseModifyOrder(lineScanner));
				break;
			case 'I':
				orderDataList.add(parseImbalanceOrder(lineScanner));
				break;
			default:
				break;
			}

		}
	}

	public OrderDatum parseAddOrder(Scanner lineScanner) {
		String sequenceNum = lineScanner.next();
		String orderReferenceNum = lineScanner.next();
		char exchangeCode = lineScanner.next().charAt(0);
		boolean isBuy = (lineScanner.next().charAt(0) == 'B') ? true : false;
		int quantity = lineScanner.nextInt();
		String stockSymbol = lineScanner.next();
		Price price = new Price(lineScanner.nextDouble());
		TimeStamp timestamp = new TimeStamp(lineScanner.nextInt() * 1000
				+ lineScanner.nextInt());
		char systemCode = lineScanner.next().charAt(0);
		String quoteId = lineScanner.next();

		OrderDatum orderData = new OrderDatum('A', sequenceNum,
				orderReferenceNum, exchangeCode, stockSymbol, timestamp,
				systemCode, quoteId, price, quantity, isBuy);

		return orderData;
	}

	public OrderDatum parseDeleteOrder(Scanner lineScanner) {
		String sequenceNum = lineScanner.next();
		String orderReferenceNum = lineScanner.next();
		TimeStamp timestamp = new TimeStamp(lineScanner.nextInt() * 1000
				+ lineScanner.nextInt());
		String stockSymbol = lineScanner.next();
		char exchangeCode = lineScanner.next().charAt(0);
		char systemCode = lineScanner.next().charAt(0);
		String quoteId = lineScanner.next();
		boolean isBuy = (lineScanner.next().charAt(0) == 'B') ? true : false;

		OrderDatum orderData = new OrderDatum('D', sequenceNum,
				orderReferenceNum, exchangeCode, stockSymbol, timestamp,
				systemCode, quoteId, new Price(0), // price doesnt matter since
													// delete
				0, // quantity as well
				isBuy);

		return orderData;

	}

	public OrderDatum parseModifyOrder(Scanner lineScanner) {
		String sequenceNum = lineScanner.next();
		String orderReferenceNum = lineScanner.next();
		int quantity = lineScanner.nextInt();
		Price price = new Price(lineScanner.nextDouble());
		TimeStamp timestamp = new TimeStamp(lineScanner.nextInt() * 1000
				+ lineScanner.nextInt());
		String stockSymbol = lineScanner.next();
		char exchangeCode = lineScanner.next().charAt(0);
		char systemCode = lineScanner.next().charAt(0);
		String quoteId = lineScanner.next();
		boolean isBuy = (lineScanner.next().charAt(0) == 'B') ? true : false;

		OrderDatum orderData = new OrderDatum('M', sequenceNum,
				orderReferenceNum, exchangeCode, stockSymbol, timestamp,
				systemCode, quoteId, price, quantity, isBuy);

		return orderData;

	}

	public OrderDatum parseImbalanceOrder(Scanner lineScanner) {
		String sequenceNum = lineScanner.next();
		String stockSymbol = lineScanner.next();
		Price price = new Price(lineScanner.nextDouble());
		int quantity = lineScanner.nextInt();
		int totalImbalance = lineScanner.nextInt();
		TimeStamp timestamp = new TimeStamp(lineScanner.nextInt() * 1000
				+ lineScanner.nextInt());
		int marketImbalance = lineScanner.nextInt();
		char auctionType = lineScanner.next().charAt(0);
		int auctionTime = lineScanner.nextInt();
		char exchangeCode = lineScanner.next().charAt(0);
		char systemCode = lineScanner.next().charAt(0);
		String quoteId = "", orderReferenceNum = "";
		boolean isBuy = false;
		OrderDatum orderData = new OrderDatum('I', sequenceNum,
				orderReferenceNum, exchangeCode, stockSymbol, timestamp,
				systemCode, quoteId, price, quantity, isBuy);
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
	
	public List<OrderDatum> getOrderDataList() {
		return orderDataList;
	}
}