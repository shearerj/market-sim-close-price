package entity.agent;

import static logger.Logger.log;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Random;
import java.util.Scanner;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import systemmanager.Keys;
import activity.Activity;
import activity.ProcessQuote;
import activity.SendToIP;
import activity.SubmitNMSOrder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Iterables;

import data.DummyFundamental;
import data.EntityProperties;
import data.FundamentalValue;
import data.OrderDatum;
import data.OrderParserNYSE;
import entity.agent.DummyAgent;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.MockMarket;
import entity.market.Order;
import entity.market.Price;
import event.TimeStamp;

public class OrderParserNYSETest {
	@BeforeClass
	public static void setupClass() {
		// Setting up the log file
		Logger.setup(3, new File("simulations/unit_testing/OrderParserTest.log"));		
	}

	private OrderParserNYSE addParser()) {
		OrderParserNYSE parser = new OrderParserNYSE();
		return parser;
	}
	
	private OrderParserNYSE addParser(String filename) {
		OrderParserNYSE parser = new OrderParserNYSE(filename);
		return parser;
	}
	
	@Test
	public void parseAddOrderTest(){
		OrderParserNYSE orderParser = addParser();
		
		String orderType = "A";
		String sequenceNum = "1"
		String orderReferenceNum = "123456789"
		String exchangeCode = "B";
		String buy = "B";
		String quantity = "1000";
		String stockSymbol = "A";
		String price = "0";
		String milliseconds = "1000";
		String seconds = "1000";
		String systemCode = "B";
		String quoteId = "A";
		
		String addOrderTest = orderType + "," +
							  sequenceNum + "," +
							  orderReferenceNum + "," +
							  exchangeCode + "," +
							  buy + "," +
							  quantity + "," +
							  stockSymbol + "," +
							  price + "," +
							  milliseconds + "," +
							  seconds + "," +
							  systemCode + "," +
							  quoteId + ",";
				
				
		Scanner lineScanner = new Scanner(addOrderTest);
		OrderDatum orderData = orderParser.parseAddOrder(lineScanner);
        
		assertTrue("Add order message type wrong", orderData.getMessageType().equals(orderType));
        assertTrue("Add order sequence num wrong", orderData.getSequenceNum().equals(sequenceNum));
 		assertTrue("Add order reference num wrong", orderData.getOrderReferenceNum().equals(orderReferenceNum));
 		assertTrue("Add order exchange code wrong", orderData.getExchangeCode().equals(exchangeCode));
 		assertTrue("Add order stock symbol wrong", orderData.getStockSymbol().equals(stockSymbol));
 		assertTrue("Add order time stamp wrong", orderData.getTimestamp().equals((milliseconds * 1000) + seconds));
 		assertTrue("Add order system code wrong", orderData.getSystemCode().equals(systemCode));
 		assertTrue("Add order quote id wrong", orderData.getQuoteId().equals(quoteId));
 		assertTrue("Add order price wrong", orderData.getPrice().equals(price));
 		assertTrue("Add order quantity wrong", orderData.getQuantity().equals(quantity));
 		assertTrue("Add order is buy wrong", orderData.isBuy().equals(buy));
    }

	@Test
	public void parseDeleteOrderTest(){
		OrderParserNYSE orderParser = addParser();
		
		String orderType = "D";
		String sequenceNum = "1"
		String orderReferenceNum = "123456789"
		String milliseconds = "1000";
		String seconds = "1000";
		String stockSymbol = "A";
		String exchangeCode = "B";
		String systemCode = "B";
		String quoteId = "A";
		String buy = "S";
		String quantity = "0";
		String price = "0";

		String deleteOrderTest = orderType + "," +
							  sequenceNum + "," +
							  orderReferenceNum + "," +
							  milliseconds + "," +
							  seconds + "," +
							  stockSymbol + "," +
							  exchangeCode + "," +
							  systemCode + "," +
							  quoteId + "," + 
							  buy + "," +
							  quantity + "," +
							  price + ",";

		Scanner lineScanner = new Scanner(deleteOrderTest);
		OrderDatum orderData = orderParser.parseDeleteOrder(lineScanner);
        
		assertTrue("Delete order message type wrong", orderData.getMessageType().equals(orderType));
        assertTrue("Delete order sequence num wrong", orderData.getSequenceNum().equals(sequenceNum));
 		assertTrue("Delete order reference num wrong", orderData.getOrderReferenceNum().equals(orderReferenceNum));
 		assertTrue("Delete order exchange code wrong", orderData.getExchangeCode().equals(exchangeCode));
 		assertTrue("Delete order stock symbol wrong", orderData.getStockSymbol().equals(stockSymbol));
 		assertTrue("Delete order time stamp wrong", orderData.getTimestamp().equals((milliseconds * 1000) + seconds));
 		assertTrue("Delete order system code wrong", orderData.getSystemCode().equals(systemCode));
 		assertTrue("Delete order quote id wrong", orderData.getQuoteId().equals(quoteId));
 		assertTrue("Delete order price wrong", orderData.getPrice().equals(price));
 		assertTrue("Delete order quantity wrong", orderData.getQuantity().equals(quantity));
 		assertTrue("Delete order is buy wrong", orderData.isBuy().equals(buy));
	}
	
	@Test
	public void parseModifyOrderTest(){
		OrderParserNYSE orderParser = addParser();
		
		String orderType = "M";
		String sequenceNum = "1"
		String orderReferenceNum = "123456789"
		String quantity = "1000";
		String price = "1000";
		String milliseconds = "1000";
		String seconds = "1000";
		String stockSymbol = "A";
		String exchangeCode = "B";
		String systemCode = "B";
		String quoteId = "A";
		String buy = "B";
		
		String addOrderTest = orderType + "," +
							  sequenceNum + "," +
							  orderReferenceNum + "," +
							  quantity + "," +
							  price + "," +
							  milliseconds + "," +
							  seconds + "," +
							  stockSymbol + "," +
							  exchangeCode + "," +
							  systemCode + "," +
							  quoteId + "," + 
							  buy + ",";
				
				
		Scanner lineScanner = new Scanner(addOrderTest);
		OrderDatum orderData = orderParser.parseModifyOrder(lineScanner);
        
		assertTrue("Modify order message type wrong", orderData.getMessageType().equals(orderType));
        assertTrue("Modify order sequence num wrong", orderData.getSequenceNum().equals(sequenceNum));
 		assertTrue("Modify order reference num wrong", orderData.getOrderReferenceNum().equals(orderReferenceNum));
 		assertTrue("Modify order exchange code wrong", orderData.getExchangeCode().equals(exchangeCode));
 		assertTrue("Modify order stock symbol wrong", orderData.getStockSymbol().equals(stockSymbol));
 		assertTrue("Modify order time stamp wrong", orderData.getTimestamp().equals((milliseconds * 1000) + seconds));
 		assertTrue("Modify order system code wrong", orderData.getSystemCode().equals(systemCode));
 		assertTrue("Modify order quote id wrong", orderData.getQuoteId().equals(quoteId));
 		assertTrue("Modify order price wrong", orderData.getPrice().equals(price));
 		assertTrue("Modify order quantity wrong", orderData.getQuantity().equals(quantity));
 		assertTrue("Modify order is buy wrong", orderData.isBuy().equals(buy));
	}
	
	@Test
	public void parseImbalanceOrderTest(){
		OrderParserNYSE orderParser = addParser();
		
		String orderType = "I";
		String sequenceNum = "1"
		String stockSymbol = "A";
		String price = "1000";
		String quantity = "1000";
		String totalImbalance = "1000";
		String milliseconds = "1000";
		String seconds = "1000";
		String marketImbalance = "1000";
		String auctionType = "O"
        String auctionTime = "0823";
		String exchangeCode = "B";
		String systemCode = "B";
		String quoteId = "A";
		String buy = "B";

		String imbalanceOrderTest = orderType + "," +
                                  sequenceNum + "," +
                                  stockSymbol + "," +
                                  price + "," +
                                  quantity + "," +
                                  totalImbalance + "," +
                                  milliseconds + "," +
                                  seconds + "," +
                                  marketImbalance + "," +
                                  auctionType + "," +
                                  auctionTime + "," +
                                  exchangeCode + "," +
                                  systemCode + "," +
                                  quoteId + "," + 
                                  buy + ",";
				
				
		Scanner lineScanner = new Scanner(addOrderTest);
		OrderDatum orderData = orderParser.parseImbalanceOrder(lineScanner);
        
		assertTrue("Imbalance order message type wrong", orderData.getMessageType().equals(orderType));
        assertTrue("Imbalance order sequence num wrong", orderData.getSequenceNum().equals(sequenceNum));
 		assertTrue("Imbalance order reference num wrong", orderData.getOrderReferenceNum().equals(orderReferenceNum));
 		assertTrue("Imbalance order exchange code wrong", orderData.getExchangeCode().equals(exchangeCode));
 		assertTrue("Imbalance order stock symbol wrong", orderData.getStockSymbol().equals(stockSymbol));
 		assertTrue("Imbalance order time stamp wrong", orderData.getTimestamp().equals((milliseconds * 1000) + seconds));
 		assertTrue("Imbalance order system code wrong", orderData.getSystemCode().equals(systemCode));
 		assertTrue("Imbalance order quote id wrong", orderData.getQuoteId().equals(quoteId));
 		assertTrue("Imbalance order price wrong", orderData.getPrice().equals(price));
 		assertTrue("Imbalance order quantity wrong", orderData.getQuantity().equals(quantity));
 		assertTrue("Imbalance order is buy wrong", orderData.isBuy().equals(buy));
	}
	
	@Test
	public void processTest(){}	
}
