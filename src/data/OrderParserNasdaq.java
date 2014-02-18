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

public class OrderParserNasdaq implements OrderParser {
	
	public OrderParserNasdaq() {

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
//		List<OrderDatum> orderDatumList = new ArrayList<OrderDatum>();
//		Scanner scanner = new Scanner(inputFile);
//
//		while (scanner.hasNextLine()) {
//			Scanner scanner2 = new Scanner(scanner.nextLine());
//			Scanner lineScanner = scanner2.useDelimiter(",");
//			scanner2.close();
//
//			char messageType = lineScanner.next().charAt(0);
//
//			switch (messageType) {
//			case 'A': // add order - no MPID attribution msg
//			case 'F': // add order with MPID attribution message TODO figure out
//				// what an MPID attribution message is
//				orderDatumList.add(parseAddOrder(lineScanner));
//				break;
//			case 'D':
//				orderDatumList.add(parseDeleteOrder(lineScanner));
//				break;
//			case 'E': // modify order executed message
//				// parseNasdaqModifyOrderExecuted(lineScanner);
//				break;
//			case 'C': // modify order executed with price message
//				// parseNasdaqModifyOrderExecutedPrice(lineScanner);
//				break;
//			case 'X': // order cancel message
//				// parseNasdaqModifyOrderCancel(lineScanner);
//				break;
//			case 'U': // order replace message
//				orderDatumList.add(parseModifyOrder(lineScanner));
//				break;
//				/*
//				 * case 'I': parseNasdaqImbalanceOrder(lineScanner); break;
//				 */
//			case 'T': // standalone timestamp
//			case 'S': // systems event message
//			case 'R': // stock directory message
//			case 'H': // stock trading action message
//			case 'Y': // Reg SHO short sale price test restricted indicator
//			case 'L': // market participant position message
//			case 'P': // trade message
//			case 'Q': // cross trade message
//			case 'B': // broken trade message
//			case 'N': // retail interest message
//			default:
//				break;
//			}
//			lineScanner.close();
//
//		}
//
//		scanner.close();
//
//	}

	public OrderDatum parseAddOrder(Scanner lineScanner) {
		TimeStamp timestamp = TimeStamp.create(lineScanner.nextInt() * 1000
				+ lineScanner.nextInt());
		String orderReferenceNum = lineScanner.next();
		OrderType type = (lineScanner.next().charAt(0) == 'B') ? BUY : SELL;
		int quantity = lineScanner.nextInt();
		String stockSymbol = lineScanner.next().trim();
		Price price = new Price(lineScanner.nextDouble());

		// These values are not available for nasdaq
		// no such thing as a sequence number in nasdaq, set it to 0 for now
		// TODO figure out what we want to do with this discrepency
		// XXX Erik: Sequentially increment sequence number?
		String sequenceNum = "";
		char exchangeCode = ' ';
		char systemCode = ' ';
		String quoteId = "";

		OrderDatum orderData = new OrderDatum('A', sequenceNum,
				orderReferenceNum, exchangeCode, stockSymbol, timestamp,
				systemCode, quoteId, price, quantity, type);

		return orderData;
	}

	public OrderDatum parseDeleteOrder(Scanner lineScanner) {
		TimeStamp timestamp = TimeStamp.create(lineScanner.nextInt() * 1000
				+ lineScanner.nextInt());
		String orderReferenceNum = lineScanner.next();

		// these values aren't used
		String sequenceNum = "";
		String stockSymbol = "";
		char exchangeCode = ' ';
		char systemCode = ' ';
		String quoteId = "";
		OrderType type = SELL; // doesn't matter because not used

		OrderDatum orderData = new OrderDatum('D', sequenceNum,
				orderReferenceNum, exchangeCode, stockSymbol, timestamp,
				systemCode, quoteId, new Price(0), // price doesnt matter since
				// delete
				0, // quantity as well
				type);

		return orderData;
	}

	public OrderDatum parseModifyOrder(Scanner lineScanner) {
		TimeStamp timestamp = TimeStamp.create(lineScanner.nextInt() * 1000
				+ lineScanner.nextInt());
		String originalOrderReferenceNum = lineScanner.next();
		// String newOrderReferenceNum = lineScanner.next(); // XXX not used
		int quantity = lineScanner.nextInt();
		Price price = new Price(lineScanner.nextDouble());

		String sequenceNum = "";
		char exchangeCode = ' ';
		String stockSymbol = "";
		char systemCode = ' ';
		String quoteId = "";
		OrderType type = SELL; // doesn't matter because not used

		OrderDatum orderData = new OrderDatum('M', sequenceNum,
				originalOrderReferenceNum, exchangeCode, stockSymbol,
				timestamp, systemCode, quoteId, price, quantity, type);

		return orderData;
	}

	public OrderDatum parseImbalanceOrder(Scanner lineScanner) {
		String sequenceNum = lineScanner.next();
		String stockSymbol = lineScanner.next();
		Price price = new Price(lineScanner.nextDouble());
		int quantity = lineScanner.nextInt();
		int totalImbalance = lineScanner.nextInt();
		TimeStamp timestamp = TimeStamp.create(lineScanner.nextInt() * 1000
				+ lineScanner.nextInt());
		int marketImbalance = lineScanner.nextInt();
		char auctionType = lineScanner.next().charAt(0);
		int auctionTime = lineScanner.nextInt();
		char exchangeCode = lineScanner.next().charAt(0);
		char systemCode = lineScanner.next().charAt(0);
		String quoteId = "", orderReferenceNum = "";
		OrderType type = SELL; // doesn't matter because not used
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

//	public List<OrderDatum> getOrderDataList() {
//		return orderDataList;
//	}
}
