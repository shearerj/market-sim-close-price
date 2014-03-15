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
	private String symbol;
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
			String orderReferenceNum, char exchangeCode, String symbol,
			TimeStamp timestamp, char systemCode, String quoteId, Price price,
			int quantity, OrderType type) {
		this.messageType = messageType;
		this.sequenceNum = sequenceNum;
		this.orderReferenceNum = orderReferenceNum;
		this.exchangeCode = exchangeCode;
		this.symbol = symbol;
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

	public String getSequenceNum() {
		return sequenceNum;
	}

	public String getOrderReferenceNum() {
		return orderReferenceNum;
	}

	public char getExchangeCode() {
		return exchangeCode;
	}

	public String getSymbol() {
		return symbol;
	}

	public TimeStamp getTimeStamp() {
		return timestamp;
	}

	public char getSystemCode() {
		return systemCode;
	}


	public String getQuoteId() {
		return quoteId;
	}

	public Price getPrice() {
		return price;
	}

	public int getQuantity() {
		return quantity;
	}

	public OrderType getOrderType() {
		return type;
	}

	public void setTotalImbalance(int totalImbalance) {
		this.totalImbalance = totalImbalance;
	}


	public void setMarketImbalance(int marketImbalance) {
		this.marketImbalance = marketImbalance;
	}


	public void setAuctionType(char auctionType) {
		this.auctionType = auctionType;
	}


	public void setAuctionTime(int auctionTime) {
		this.auctionTime = auctionTime;
	}


	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public int getTotalImbalance() {
		return totalImbalance;
	}

	public int getMarketImbalance() {
		return marketImbalance;
	}

	public char getAuctionType() {
		return auctionType;
	}


	public int getAuctionTime() {
		return auctionTime;
	}
}
