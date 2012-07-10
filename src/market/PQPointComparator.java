package market;

import java.util.Comparator;

/**
 * Comparator class for PQPoints
 * <ul>
 * <li>Options are to sort on price/quantity/timestamp, in any order
 * and in any order of importance.  
 * <li>The constructor takes an integer for each of Price, Quantity, timestamp,
 * the sign of each arguments indicates whether to sort that field in
 * ascending (+) descending (-) or non-ordered (0), and the relative
 * magnitudes of the three fields designate the order of importance of each,
 * where highest magnitude (abs value) will be the first fields on which to sort.
 * <li> eg:  parg = 3, qarg = 2, targ = -1 will sort
 * first in ascending price, breaking ties in ascending quantity, breaking
 * remaining ties in descending timestamps.
 * </ul>
 */
public class PQPointComparator implements Comparator
{
	private int pOrder; //ascending/descending
	private int qOrder;
	private int tOrder;

	/**
	 * the relative magnitudes of the three arguments determines the
	 * order in which the sorts will be performed, where highest
	 * magnitude takes precedence for the comparison.
	 *
	 * @param pAscending > 0 sorts in ascending price, < 0 descending
	 * @param qAscending > 0 sorts in ascending quantity, < 0 descending
	 * @param tAscending > 0 sorts in ascending timestamp . . .

	 */
	public PQPointComparator(int pAscending, int qAscending, int tAscending)
	{
		pOrder =  pAscending;
		qOrder =  qAscending;
		tOrder =  tAscending;
	}

	/** default is to sort in descending order of price/ inc date, dec quantity
	 */
	public PQPointComparator()
	{ 
		pOrder = -3;
		qOrder = -1;
		tOrder = 2;
	}

	/**
	 * compare to points based on default comparator
	 *
	 * @param o1 object 1
	 * @param o2 object to compare to,  
	 * @return 1,0,-1 if o1 is >=< o2
	 */
	public int compare(Object o1, Object o2)
	{
		PQPoint pq1 = (PQPoint)o1;
		PQPoint pq2 = (PQPoint)o2;
		int P,Q,T,O;

		//compare on price
		P = pq1.getPrice().compareTo(pq2.getPrice());

		//compare on quantity
		Q = (new Integer(pq1.getQuantity())).compareTo(new Integer(pq2.getQuantity()));

		if ((pq1.Parent != null) && (pq1.Parent.timestamp != null) &&
				(pq2.Parent != null) && (pq2.Parent.timestamp != null))
		{
			T = (pq1.Parent.timestamp.compareTo(pq2.Parent.timestamp));

			if (T == 0)
				T = pq1.Parent.bidID.compareTo(pq2.Parent.bidID);
		}
		else
		{
			T = 0;
		}

		//use the hashcodes as a last resort
		O = new Integer((o1.hashCode())).compareTo(new Integer(o2.hashCode()));

		//this will be <=> 0 based on comparisons and rankings of fields
		int ret = 2*(pOrder*P+qOrder*Q+tOrder*T) + O ;

		return (ret);
	}
}
