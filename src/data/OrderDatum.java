package data;

import java.io.Serializable;

import entity.market.Price;
import event.TimeStamp;
import fourheap.Order.OrderType;

public class OrderDatum implements Serializable {

	private static final long serialVersionUID = 6379805806017545016L;

	private char messageType;
	private String sequenceNum;
	private String orderReferenceNum;
	private char exchangeCode;
	private String stockSymbol;
	private TimeStamp timestamp;
	private char systemCode;
	private String quoteId;
	private Price price; // 10 bytes in nyse
	private int quantity;//9 bytes in nyse
	private int totalImbalance;
	private int marketImbalance;
	private char auctionType;
	private int auctionTime;
	private OrderType type;


	public OrderDatum(char messageType, String sequenceNum,
			String orderReferenceNum, char exchangeCode, String stockSymbol,
			TimeStamp timestamp, char systemCode, String quoteId, Price price,
			int quantity, OrderType type) {
		this.messageType = messageType;
		this.sequenceNum = sequenceNum;
		this.orderReferenceNum = orderReferenceNum;
		this.exchangeCode = exchangeCode;
		this.stockSymbol = stockSymbol;
		this.timestamp = timestamp;
		this.systemCode = systemCode;
		this.quoteId = quoteId;
		this.price = price;
		this.quantity = quantity;
		this.type = type;
	}


	public char getMessageType() {
		return messageType;
	}

	public String getMessageTypeAsString() {
		return Character.toString(messageType);
	}


	public void setMessageType(char messageType) {
		this.messageType = messageType;
	}


	public String getSequenceNum() {
		return sequenceNum;
	}


	public void setSequenceNum(String sequenceNum) {
		this.sequenceNum = sequenceNum;
	}


	public String getOrderReferenceNum() {
		return orderReferenceNum;
	}


	public void setOrderReferenceNum(String orderReferenceNum) {
		this.orderReferenceNum = orderReferenceNum;
	}


	public char getExchangeCode() {
		return exchangeCode;
	}


	public void setExchangeCode(char exchangeCode) {
		this.exchangeCode = exchangeCode;
	}


	public String getStockSymbol() {
		return stockSymbol;
	}


	public void setStockSymbol(String stockSymbol) {
		this.stockSymbol = stockSymbol;
	}


	public TimeStamp getTimestamp() {
		return timestamp;
	}


	public void setTimestamp(TimeStamp timestamp) {
		this.timestamp = timestamp;
	}


	public char getSystemCode() {
		return systemCode;
	}


	public void setSystemCode(char systemCode) {
		this.systemCode = systemCode;
	}


	public String getQuoteId() {
		return quoteId;
	}


	public void setQuoteId(String quoteId) {
		this.quoteId = quoteId;
	}


	public Price getPrice() {
		return price;
	}


	public void setPrice(Price price) {
		this.price = price;
	}


	public int getQuantity() {
		return quantity;
	}


	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}


	public OrderType getType() {
		return type;
	}


	public void setBuy(OrderType type) {
		this.type = type;
	}


	public static long getSerialversionuid() {
		return serialVersionUID;
	}


	public int getTotalImbalance() {
		return totalImbalance;
	}


	public void setTotalImbalance(int totalImbalance) {
		this.totalImbalance = totalImbalance;
	}


	public int getMarketImbalance() {
		return marketImbalance;
	}


	public void setMarketImbalance(int marketImbalance) {
		this.marketImbalance = marketImbalance;
	}


	public char getAuctionType() {
		return auctionType;
	}


	public void setAuctionType(char auctionType) {
		this.auctionType = auctionType;
	}


	public int getAuctionTime() {
		return auctionTime;
	}


	public void setAuctionTime(int auctionTime) {
		this.auctionTime = auctionTime;
	}
}
