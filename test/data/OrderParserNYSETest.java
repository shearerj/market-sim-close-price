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
		List<OrderDatum> orderDataList = orderParser.parseAddOrder(lineScanner);
		
		//TODO check to see if the results are correct
	}
	
	@Test
	public void parseDeleteOrderTest(){
		String filename = "OrderAddTestNYSE.txt"
				OrderParserNYSE orderParser = addParser();
				
				List<OrderDatum> orderDataList = orderParser.parseAddOrder(lineScanner);
	}
	
	@Test
	public void parseModifyOrderTest(){
		String filename = "OrderAddTestNYSE.txt"
				OrderParserNYSE orderParser = addParser();
				
				List<OrderDatum> orderDataList = orderParser.parseAddOrder(lineScanner);
	}
	
	@Test
	public void parseImbalanceOrderTest(){
		String filename = "OrderAddTestNYSE.txt"
				OrderParserNYSE orderParser = addParser();
				
				List<OrderDatum> orderDataList = orderParser.parseAddOrder(lineScanner);
	}
	
	@Test
	public void processTest(){}	
}
