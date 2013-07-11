package market;

import java.util.Comparator;

/**
 * Compares two transactions via transaction ID only.
 * 
 * @author ewah
 */
public class TransactionIDComparator implements Comparator<Transaction> {

	@Override
	public int compare(Transaction o1, Transaction o2) {
		return (int) Math.signum(o1.getTransID() - o2.getTransID());
	}
	
}
