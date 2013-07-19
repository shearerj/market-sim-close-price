package market;

import java.util.Comparator;

/**
 * Comparator class for PQPoints
 * 
 * Options are to sort on price/quantity/timestamp, in any order and in any order of importance. The
 * constructor takes an integer for each of Price, Quantity, timestamp, the sign of each arguments
 * indicates whether to sort that field in ascending (+) descending (-) or non-ordered (0), and the
 * relative magnitudes of the three fields designate the order of importance of each, where highest
 * magnitude (abs value) will be the first fields on which to sort. eg: parg = 3, qarg = 2, targ =
 * -1 will sort first in ascending price, breaking ties in ascending quantity, breaking remaining
 * ties in descending timestamps.
 * 
 */
public class PQPointComparator implements Comparator<PQPoint> {
	private int pOrder; // ascending/descending
	private int qOrder;
	private int tOrder;

	/**
	 * the relative magnitudes of the three arguments determines the order in which the sorts will
	 * be performed, where highest magnitude takes precedence for the comparison.
	 * 
	 * @param pAscending
	 *            > 0 sorts in ascending price, < 0 descending
	 * @param qAscending
	 *            > 0 sorts in ascending quantity, < 0 descending
	 * @param tAscending
	 *            > 0 sorts in ascending timestamp . . .
	 */
	public PQPointComparator(int pAscending, int qAscending, int tAscending) {
		// This takes 2 to the power of, so that if the highest ranked one has a certain sign, then
		// the comparison will also be required to have that result. Before that wasn't the case.
		pOrder = power2(pAscending);
		qOrder = power2(qAscending);
		tOrder = power2(tAscending);
	}

	/**
	 * default is to sort in descending order of price/ inc date, dec quantity
	 */
	public PQPointComparator() {
		pOrder = -3;
		qOrder = -1;
		tOrder = 2;
	}

	/**
	 * Takes two to the power of the argument, but keeps the sign. Kind of.
	 * 
	 * 1 => 1 -1 => -1 3 => 8 -3 => -8
	 */
	protected static int power2(int x) {
		return Integer.signum(x) * (1 << (Math.abs(x) - 1));
	}

	/**
	 * compare to points based on default comparator
	 * 
	 * @param o1
	 *            object 1
	 * @param o2
	 *            object to compare to,
	 * @return 1,0,-1 if o1 is >=< o2
	 */
	@Override
	public int compare(PQPoint pq1, PQPoint pq2) {
		int price, quantity, time;

		// compare on price
		price = Integer.signum(pq1.getPrice().compareTo(pq2.getPrice()));

		// compare on quantity
		quantity = Integer.signum(pq1.getQuantity() - pq2.getQuantity());

		if ((pq1.Parent != null) && (pq1.Parent.submitTime != null)
				&& (pq2.Parent != null) && (pq2.Parent.submitTime != null)) {
			time = Integer.signum(pq1.Parent.submitTime.compareTo(pq2.Parent.submitTime));
		} else {
			time = 0;
		}

		// this will be <=> 0 based on comparisons and rankings of fields
		return pOrder * price + qOrder * quantity + tOrder * time;
	}

}
