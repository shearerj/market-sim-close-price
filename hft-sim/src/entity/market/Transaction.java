package entity.market;

import java.io.Serializable;

import event.TimeStamp;

/**
 * Base class for Transactions. Contains information on buyer/seller, market, quantity, price, and
 * time.
 * 
 * @author ewah
 */
public class Transaction implements Serializable {

	private static final long serialVersionUID = 8420827805792281642L;

	// Transaction Info
	private final int quantity;
	private final Price price;
	private final TimeStamp execTime;

	protected Transaction(int quantity, Price price, TimeStamp execTime) {
		this.quantity = quantity;
		this.price = price;
		this.execTime = execTime;
	}

	public final int getQuantity() {
		return quantity;
	}

	public final Price getPrice() {
		return price;
	}

	public final TimeStamp getExecTime() {
		return execTime;
	}

	@Override
	public final int hashCode() {
		return super.hashCode();
	}

	@Override
	public final boolean equals(Object obj) {
		return super.equals(obj);
	}

	@Override
	public String toString() {
		return quantity + " @ " + price;
	}

}
