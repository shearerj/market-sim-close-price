package data;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import logger.Log;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Lists;

import systemmanager.Consts;
import data.OrderDatum;
import data.OrderParserNYSE;
import entity.market.Price;
import event.TimeStamp;
import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;

public class OrderParserNYSETest {
	
	@BeforeClass
	public static void setupClass() throws IOException {
		// Setting up the log file
		Log.log = Log.create(Log.Level.DEBUG, new File(Consts.TEST_OUTPUT_DIR + "OrderParserTest.log"));		
	}

	private OrderParserNYSE addParser() {
		OrderParserNYSE parser = new OrderParserNYSE();
		return parser;
	}
	
	@Test
	public void parseAddOrderTest2() {
		OrderParserNYSE orderParser = new OrderParserNYSE();
		String line = "A,1,123456789,B,B,1000,A,0,1000,10,B,A\n";
		
	}

//	private OrderParserNYSE addParser(String filename) {
//		OrderParserNYSE parser = null;
//		try {
//			parser = new OrderParserNYSE();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		return parser;
//	}

	@Test
	public void parseAddOrderTest(){
		OrderParserNYSE orderParser = addParser();

		String orderType = "A";
		String sequenceNum = "1";
		String orderReferenceNum = "123456789";
		String exchangeCode = "B";
		String buy = "B";	// true
		String quantity = "1000";
		String stockSymbol = "A";
		String price = "0";
		String milliseconds = "1000";
		String seconds = "10";
		String systemCode = "B";
		String quoteId = "A";

		String addOrderString = orderType + "," +
				sequenceNum + "," +
				orderReferenceNum + "," +
				exchangeCode + "," +
				buy + "," +
				quantity + "," +
				stockSymbol + "," +
				price + "," +
				seconds + "," +
				milliseconds + "," +
				systemCode + "," +
				quoteId + ",        ";


		String testType = "Add";
		List<String> testString = Lists.newArrayList(addOrderString.split(","));
		OrderDatum orderData = orderParser.parseAddOrder(testString);

		assertEquals(testType + " order sequence num wrong", sequenceNum, orderData.getSequenceNum());
		assertEquals(testType + " order reference num wrong", orderReferenceNum, orderData.getOrderReferenceNum());
		assertEquals(testType + " order exchange code wrong", exchangeCode, 
				Character.toString(orderData.getExchangeCode()));
		assertEquals(testType + " order stock symbol wrong", stockSymbol, orderData.getSymbol());
		assertEquals(testType + " order time stamp wrong", 
				TimeStamp.create(Integer.parseInt(milliseconds) * 1000 + Integer.parseInt(seconds)),
				orderData.getTimeStamp());
		assertEquals(testType + " order system code wrong", systemCode, 
				Character.toString(orderData.getSystemCode()));
		assertEquals(testType + " order quote id wrong", quoteId, orderData.getQuoteId());
		assertEquals(testType + " order price wrong", new Price(Integer.parseInt(price)), orderData.getPrice());
		assertEquals(testType + " order quantity wrong", Integer.parseInt(quantity), orderData.getQuantity());
		assertEquals(testType + " order is buy wrong", BUY, orderData.getOrderType());

	}

	@Test
	public void parseDeleteOrderTest(){
		OrderParserNYSE orderParser = addParser();

		String orderType = "D";
		String sequenceNum = "1";
		String orderReferenceNum = "123456789";
		String milliseconds = "1000";
		String seconds = "10";
		String stockSymbol = "A";
		String exchangeCode = "B";
		String systemCode = "B";
		String quoteId = "A";
		String buy = "S";	// false
		String quantity = "0";
		String price = "0";

		String deleteOrderTest = orderType + "," +
				sequenceNum + "," +
				orderReferenceNum + "," +
				seconds + "," +
				milliseconds + "," +
				stockSymbol + "," +
				exchangeCode + "," +
				systemCode + "," +
				quoteId + "," + 
				buy + "," +
				quantity + "," +
				price + ",";

		String testType = "Delete";
		Scanner lineScanner = new Scanner(deleteOrderTest);
		lineScanner.useDelimiter(",");
		assertEquals(testType + " order message type wrong", orderType, lineScanner.next());
		OrderDatum orderData = orderParser.parseDeleteOrder(lineScanner);

		assertEquals(testType + " order sequence num wrong", sequenceNum, orderData.getSequenceNum());
		assertEquals(testType + " order reference num wrong", orderReferenceNum, orderData.getOrderReferenceNum());
		assertEquals(testType + " order exchange code wrong", exchangeCode, 
				Character.toString(orderData.getExchangeCode()));
		assertEquals(testType + " order stock symbol wrong", stockSymbol, orderData.getSymbol());
		assertEquals(testType + " order time stamp wrong", 
				TimeStamp.create(Integer.parseInt(milliseconds) * 1000 + Integer.parseInt(seconds)),
				orderData.getTimeStamp());
		assertEquals(testType + " order system code wrong", systemCode, 
				Character.toString(orderData.getSystemCode()));
		assertEquals(testType + " order quote id wrong", quoteId, orderData.getQuoteId());
		assertEquals(testType + " order price wrong", new Price(Integer.parseInt(price)), orderData.getPrice());
		assertEquals(testType + " order quantity wrong", Integer.parseInt(quantity), orderData.getQuantity());
		assertEquals(testType + " order is buy wrong", SELL, orderData.getOrderType());

		lineScanner.close();
	}

	@Test
	public void parseModifyOrderTest(){
		OrderParserNYSE orderParser = addParser();

		String orderType = "M";
		String sequenceNum = "1";
		String orderReferenceNum = "123456789";
		String quantity = "1000";
		String price = "1000";
		String milliseconds = "1000";
		String seconds = "10";
		String stockSymbol = "A";
		String exchangeCode = "B";
		String systemCode = "B";
		String quoteId = "A";
		String buy = "B";	// true

		String modifyOrderTest = orderType + "," +
				sequenceNum + "," +
				orderReferenceNum + "," +
				quantity + "," +
				price + "," +
				seconds + "," +
				milliseconds + "," +
				stockSymbol + "," +
				exchangeCode + "," +
				systemCode + "," +
				quoteId + "," + 
				buy + ",";

		String testType = "Modify";
		Scanner lineScanner = new Scanner(modifyOrderTest);
		lineScanner.useDelimiter(",");
		assertEquals(testType + " order message type wrong", orderType, lineScanner.next());
		OrderDatum orderData = orderParser.parseModifyOrder(lineScanner);

		assertEquals(testType + " order sequence num wrong", sequenceNum, orderData.getSequenceNum());
		assertEquals(testType + " order reference num wrong", orderReferenceNum, orderData.getOrderReferenceNum());
		assertEquals(testType + " order exchange code wrong", exchangeCode, 
				Character.toString(orderData.getExchangeCode()));
		assertEquals(testType + " order stock symbol wrong", stockSymbol, orderData.getSymbol());
		assertEquals(testType + " order time stamp wrong", 
				TimeStamp.create(Integer.parseInt(milliseconds) * 1000 + Integer.parseInt(seconds)),
				orderData.getTimeStamp());
		assertEquals(testType + " order system code wrong", systemCode, 
				Character.toString(orderData.getSystemCode()));
		assertEquals(testType + " order quote id wrong", quoteId, orderData.getQuoteId());
		assertEquals(testType + " order price wrong", new Price(Integer.parseInt(price)), orderData.getPrice());
		assertEquals(testType + " order quantity wrong", Integer.parseInt(quantity), orderData.getQuantity());
		assertEquals(testType + " order is buy wrong", BUY, orderData.getOrderType());

		lineScanner.close();
	}

	@Test
	public void parseImbalanceOrderTest(){
		OrderParserNYSE orderParser = addParser();

		String orderType = "I";
		String sequenceNum = "1";
		String stockSymbol = "A";
		String price = "1000";
		String quantity = "1000";
		String totalImbalance = "1000";
		String milliseconds = "1000";
		String seconds = "10";
		String marketImbalance = "1000";
		String auctionType = "O";
		String auctionTime = "0823";
		String exchangeCode = "B";
		String systemCode = "B";

		String imbalanceOrderTest = orderType + "," +
				sequenceNum + "," +
				stockSymbol + "," +
				price + "," +
				quantity + "," +
				totalImbalance + "," +
				seconds + "," +
				milliseconds + "," +
				marketImbalance + "," +
				auctionType + "," +
				auctionTime + "," +
				exchangeCode + "," +
				systemCode + ",";


		String testType = "Imbalance";
		Scanner lineScanner = new Scanner(imbalanceOrderTest);
		lineScanner.useDelimiter(",");
		assertEquals(testType + " order message type wrong", orderType, lineScanner.next());
		OrderDatum orderData = orderParser.parseImbalanceOrder(lineScanner);

		assertEquals(testType + " order sequence num wrong", sequenceNum, orderData.getSequenceNum());
		assertEquals(testType + " total imbalance wrong", Integer.parseInt(totalImbalance), orderData.getTotalImbalance());
		assertEquals(testType + " market imbalance wrong", Integer.parseInt(marketImbalance), orderData.getMarketImbalance());
		assertEquals(testType + " order exchange code wrong", exchangeCode, 
				Character.toString(orderData.getExchangeCode()));
		assertEquals(testType + " order stock symbol wrong", stockSymbol, orderData.getSymbol());
		assertEquals(testType + " order time stamp wrong", 
				TimeStamp.create(Integer.parseInt(milliseconds) * 1000 + Integer.parseInt(seconds)),
				orderData.getTimeStamp());
		assertEquals(testType + " order system code wrong", systemCode, 
				Character.toString(orderData.getSystemCode()));
		assertEquals(testType + " order price wrong", new Price(Integer.parseInt(price)), orderData.getPrice());
		assertEquals(testType + " order quantity wrong", Integer.parseInt(quantity), orderData.getQuantity());
		
		lineScanner.close();
	}

	
	public void processTest(){
		// TODO ????
	}	
}
