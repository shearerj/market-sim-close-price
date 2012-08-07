/**
 * $Id: FourHeap.java,v 1.22 2004/09/28 17:54:28 klochner Exp $
 * Copyright Information goes here
 * 
 * Edited 2012/06/08 by ewah
 */
package market;

import java.util.*;

import systemmanager.Log;

/**
 *
 * fourHeap
 * <ul>
 * <li> one heap of matching buys,
 * <li> one heap of matching sells,
 * <li> one heap each of non-matching buys/sells
 * <li> number of matching price points is equal to
 *    min(matchSellSetSize,matchBuySetSize) for buys and sells
 * </ul>
 */
public class FourHeap
{

	private static final boolean DEBUG = false;

	private SortedSet sellSet;
	private SortedSet buySet;
	public SortedSet matchSellSet;
	public SortedSet matchBuySet;

	private Comparator bestFirstB ;
	private Comparator bestFirstS;
	private Comparator worstFirstB;
	private Comparator worstFirstS;

	private Comparator betterSell;
	private Comparator betterBuy;
	private Log log;
	private Integer mktID;
	private int matchBuySetSize;          // #buys  in the matchedBuySet
	private int matchSellSetSize;         // #sells in the matchedSellSet

	public FourHeap(int mktID)
	{
		/*Comparators to sort PQPoints in increasing/decreasing price, dectime
		 *the "best" sell bid has the lowest price, earliest time
		 *    "best" buy bid has highest price, earliest time, . . .
		 */
		bestFirstB = new PQPointComparator(-2,0,1);
		bestFirstS = new PQPointComparator(2,0,1);
		worstFirstB = new PQPointComparator(2,0,-1);
		worstFirstS = new PQPointComparator(-2,0,-1);

		//comparators just to make the code more readable
		betterSell = worstFirstS;
		betterBuy = worstFirstB;

		//4 heaps
		sellSet          = new TreeSet(bestFirstS);
		buySet           = new TreeSet(bestFirstB);
		matchSellSet     = new TreeSet(worstFirstS);
		matchBuySet      = new TreeSet(worstFirstB);

		//maintain sizes for the matching heaps since PQPoints have variable quantity
		matchSellSetSize = 0;
		matchBuySetSize  = 0;

		this.mktID = mktID;
	}


	/**
	 * what is the allocation to P given current state of FourHeap
	 *P must be present in the orderbook to receive a valid alloc
	 *
	 *@param P point to evaluate
	 *@return int allocation due to P if the auction were to close
	 *
	 */
	public int pointAlloc(PQPoint P)
	{
		//only need to check one of buy or sell heap to see if no matche
		if (matchSellSetSize == 0)
			return 0;

		if (((P.getQuantity() > 0) && (betterBuy.compare(P,worstMatchingBuy()) > 0)) ||
				((P.getQuantity() < 0) && (betterSell.compare(P,worstMatchingSell()) > 0)))
		{
			return P.getQuantity();
		}
		else if (P == worstMatchingBuy())
		{
			return P.getQuantity() - Math.max(0,matchBuySetSize-matchSellSetSize);
		}
		else if (P == worstMatchingSell())
		{
			return P.getQuantity()  + Math.max(0,matchSellSetSize-matchBuySetSize);
		}

		return 0;

	}
	/**
	 * set the log and aucID of the 4Heap
	 *
	 * @param l
	 * @param aID
	 */
	public void setParams(Log l, int aID) {
		log = l; mktID = new Integer(aID);
	}
	
	
	public boolean shouldLog(int code) {
		if (log == null) {
			return false;
		} else {
			return log.shouldLog(code);
		}
	}
	public void log(int code, String msg)
	{
		if (log == null) {
			//System.out.println(code+"::"+msg);
		} else
		{
			msg = mktID.toString() + "," + msg;
			log.log(code, msg);
		}
	}
	
	/**
	 *what would an agent have to bid *under* to place winning *sell*
	 *
	 *@return current bid or -1 if there are no buy bids in the heap
	 */
	public Price getBidQuote()
	{
		// new sell either has to beat a matching sell, or
		// match a non-matching buy
		if (DEBUG)  logSets();
		Price buyToMatch = null;
		Price sellToBeat = null;
		Price ret;

		//determine the easiest non-matching buy to match
		if (matchBuySetSize > matchSellSetSize)
			buyToMatch =  worstMatchingBuy().getPrice();
		else if (bestBuy() != null)
			buyToMatch = bestBuy().getPrice();

		//determine if there is a matched sell to beat
		if (worstMatchingSell() != null)
			sellToBeat = worstMatchingSell().getPrice();

		//return the Higher of the two prices (easiest to match)
		//null is returned only if both arguments are null
		if ((buyToMatch != null) && (DEBUG))
			log(Log.DEBUG,"buyToMatch: "+buyToMatch);
		if ((sellToBeat != null) && (DEBUG))
			log(Log.DEBUG,"selltoBeat: "+sellToBeat);
		ret =  Price.max(buyToMatch,sellToBeat);
		if (ret == null)
			ret = new Price(-1);

		return ret;
	}


	/**
	 *what would an agent have to bid *over* to place a winning *buy*
	 *
	 *@return current ask or -1 if there are no sell bids in the heap
	 */
	public Price getAskQuote()
	{
		Price sellToMatch = null;
		Price buyToBeat = null;
		Price ret = null;
		if (DEBUG) logSets();
		//new buy either has to beat a matching buy, or
		// match a non-matching sell
		//compute the lowest non-winning sell price
		if (matchSellSetSize > matchBuySetSize)
			sellToMatch = worstMatchingSell().getPrice();
		else if (bestSell() != null)
			sellToMatch = bestSell().getPrice();

		//compute the lowest winning buy bid
		if (worstMatchingBuy() != null)
			buyToBeat = worstMatchingBuy().getPrice();

		//match the lower of the two (easiest buy to match)
		//null is returned only if both arguments are null;
		if (buyToBeat != null)
			log(Log.DEBUG, "buyToBeat: "+buyToBeat);
		if (sellToMatch != null)
			log(Log.DEBUG, "sellToMatch: "+sellToMatch);
		ret =  Price.min(buyToBeat, sellToMatch);

		if (ret == null)
			ret = new Price(-1);

		return ret;

	}


	/**
	 * call to remove a PQPoint from the data structure
	 *
	 * @param P PQPoint to remove
	 * must call matchBids() after this to restore 4Heap invariants
	 */
	public void removeBid(PQPoint P)
	{
		if (P.getQuantity() < 0)
		{
			//if it's supposed to be in the matchSS & we remove it, reduce matchsellsetsize
			if  ((!matchSellSet.isEmpty() && (betterSell.compare(P,worstMatchingSell()) >= 0))
					&& (matchSellSet.remove(P)))
			{
				matchSellSetSize -= Math.abs(P.getQuantity());
			}
			// otherwise, if we can't remove it from the sell set, send a debug message
			else if (sellSet.isEmpty() || (bestFirstS.compare(P,bestSell()) < 0)
					|| (!sellSet.remove(P)))
			{
				debugn("FourHeap::removebid:  didn't find the sell");
			}
		}
		else
		{
			if (!buySet.isEmpty())
				debugn("bs compare: "+P.toString()+"<>"+bestBuy().toString()+"--> "+betterBuy.compare(P,bestBuy()));
			if (worstMatchingBuy()!= null)
				debugn("compare, "+P.toString()+","+worstMatchingBuy().toString()+betterBuy.compare(P,worstMatchingBuy()));
			//if it's supposed to be in the matchBS & we remove it, reduce matchBuySetSize
			debugn("mbss: "+matchBuySet.size());
			debugn("bss: "+matchBuySet.size());

			if ((!matchBuySet.isEmpty() && (betterBuy.compare(P,worstMatchingBuy()) >= 0))
					&& (matchBuySet.remove(P)))
			{
				debugn("removed . . . size = "+matchBuySet.size()+" actual should be "+P.getQuantity()+" less");
				matchBuySetSize -= P.getQuantity();
			}
			//otherwise, if we can't remove it from the buy set, send a debug message
			else if (buySet.isEmpty() || (bestFirstB.compare(P,bestBuy()) < 0)
					|| (!buySet.remove(P)))
			{
				debugn("FourHeap::removebid:  didn't find the buy");
			}
			else {

				buySet.remove(P);

			}
		}

		debugn("after removal matchsellsetSize: "+matchSellSetSize);
		debugn("after removal matchbuysetSize: "+matchBuySetSize);
		debugn("after removal buysetSize: "+buySet.size());

		//restore our 4Heap invariants
		equalizeSetSizes();
		matchBids();
		equalizeSetSizes();
		if (DEBUG)
			logSets();
		debug("leaving remove");
	}

	/**
	 * insert a PQPoint into the 4Heap data structure
	 *
	 *@param P PQPoint to insert
	 */
	public void insertBid(PQPoint P)
	{
//		logSets();
		//determine whether P is a buy/sell
		if (P.getQuantity() < 0) {
			if ((worstMatchingSell()!=null) && (betterSell.compare(P, worstMatchingSell()) > 0)) {
				matchSellSet.add(P);
				matchSellSetSize += Math.abs(P.getQuantity());
			} else {
				sellSet.add(P);
			}
		} else if (P.getQuantity() > 0)	{
			if ((worstMatchingBuy()!=null ) && (betterBuy.compare(P, worstMatchingBuy()) > 0)) {
				matchBuySet.add(P);
				matchBuySetSize += P.getQuantity();
			} else {
				buySet.add(P);
			}
		}
//		logSets();
		equalizeSetSizes();
		matchBids();
//		logSets();
		equalizeSetSizes();
	}

	/**
	 * clear the 4Heap, returning Vectors of matched buys/sells
	 *
	 * @param buys buys will be inserted here
	 * @param sells sells will be inserted here
	 */
	public void clear(ArrayList buys, ArrayList sells)
	{
		equalizeRealSetSizes();
		buys.addAll(matchBuySet);
		sells.addAll(matchSellSet);
		matchSellSet.clear();
		matchBuySet.clear();
		matchSellSetSize = 0;
		matchBuySetSize = 0;
		if ((buys == null) || (sells == null)) debugn("error");
	}


	/**
	 * ensure that the matchsellset/matchbuyset have the same quantity
	 * splitting a PQPoint where necessary
	 */
	private void equalizeRealSetSizes()
	{
		//determine whether a bid will partially transact & split it
		int diff = matchBuySetSize - matchSellSetSize;
		if (diff < 0)
		{
			PQPoint temp = worstMatchingSell();
			matchSellSet.remove(temp);
			sellSet.add(temp.split(diff));
			matchSellSet.add(temp);
		}
		else if (diff > 0)
		{
			PQPoint temp = worstMatchingBuy();
			matchBuySet.remove(temp);
			buySet.add(temp.split(diff));
			matchBuySet.add(temp);
		}
	}

	/**
	 * what is the best non-matching buy in the heap
	 *
	 * @return the best non-matching buy
	 */
	private PQPoint bestBuy()
	{
		if (buySet.isEmpty())
			return null;
		else
			return (PQPoint)  buySet.first();
	}
	/**
	 * what is the best non-matching sell in the heap
	 *
	 * @return the best non-matching sell
	 */
	private PQPoint bestSell()
	{
		if (sellSet.isEmpty())
			return null;
		else
			return (PQPoint)  sellSet.first();
	}
	/**
	 * what is the worst matching sell in the heap
	 *
	 * @return the worst matching sell
	 */
	private PQPoint worstMatchingBuy()
	{
		if (matchBuySet.isEmpty())
			return null;
		else return (PQPoint)  matchBuySet.first();
	}
	/**
	 * what is the worst matching sell in the heap
	 *
	 * @return the worst matching sell
	 */
	private PQPoint worstMatchingSell()
	{
		if (matchSellSet.isEmpty())
			return null;
		else
			return (PQPoint)  matchSellSet.first();
	}

	/**
	 *  matchBids()
	 *
	 *  restores invariant that all matching bids are in
	 *  the matched heaps, and a minimum number of non-matching
	 *  price-points are in the matched heaps
	 */
	private void matchBids()
	{

		while(true)
		{
			//assertions to test invariants
			/*assert((bestBuy()==null) ||
	     (betterBuy.compare(worstMatchingBuy(),bestBuy() ) >= 0))
	: "buys out of order";
      assert((bestSell()==null) ||
	     (betterSell.compare(worstMatchingSell(),bestSell()) >= 0))
	: "sells out of order";
			 */

			// try to match extra sells with buy set
			if (matchSellSetSize > matchBuySetSize)
			{
				if ((bestBuy()!=null) &&
						(bestBuy().comparePrice( worstMatchingSell() ) >= 0 ))
				{
					promoteBuy();
				}
				else
					break;
			}
			// try to match extra buys with sell set
			else if (matchBuySetSize > matchSellSetSize)
			{
				if ((bestSell()!=null) &&
						(bestSell().comparePrice(worstMatchingBuy()) <= 0))
				{
					promoteSell();
				}
				else
					break;
			}
			// if no extra buys/sells, try to match from unmatched set
			else if ((bestBuy()!=null) && (bestSell()!=null) &&
					(bestBuy().comparePrice(bestSell()) >= 0))
			{
				promoteBuy();
				promoteSell();
			}
			// break if no more matching to do
			else
			{
				break;
			}
		}

		//post-match invariants
		/*assert((worstMatchingBuy()==null) ||
	   (worstMatchingBuy().comparePrice(worstMatchingSell()) >= 0))
      : "matches don't match";
    assert((bestBuy()== null) || (bestSell()==null) ||
	   (bestBuy().comparePrice(bestSell() ) < 0))
	   : "haven't made all matches";*/

	}

	/**
	 *
	 * equalizeSetSizes()
	 * call to reduce the heaps so that the larger of the heaps
	 *   hold a minimum  of extra bids
	 */
	private void equalizeSetSizes()
	{
		//remove items from the larger set to maintain
		// a minimum quantity in the matching sets
		debug("mBSetSize/mSSetSize = " + matchBuySetSize + " " + matchSellSetSize);
		try {

			while ((matchBuySetSize > matchSellSetSize) &&
					(matchBuySetSize - matchSellSetSize >=
					worstMatchingBuy().getQuantity() ))
			{
				demoteBuy();
			}
		} catch (java.lang.NullPointerException e)
		{
			logSets();
			if (worstMatchingBuy() == null)
				log(Log.ERROR,"FourHeap::equalizeSetSizes, wmb==null");
			throw(e);
		}

		try {
			while ((matchSellSetSize > matchBuySetSize) &&
					(matchSellSetSize - matchBuySetSize >=
					Math.abs(worstMatchingSell().getQuantity() )))
			{
				demoteSell();
				debug("mBSetSize/mSSetSize = " + matchBuySetSize + " " + matchSellSetSize);
			}
		} catch (java.lang.NullPointerException e)
		{
			logSets();
			if (worstMatchingSell() == null)
				log(Log.ERROR,"FourHeap::equalizeSetSizes, wms==null");

			throw(e);
		}
		debug("returning from equalizeSetSizes");

	}

	/**
	 * move one PQPoint from the buySet to the matchBuySet
	 */
	private void promoteBuy()
	{
		PQPoint temp = bestBuy();
		if (temp != null)
		{
			buySet.remove(temp);
			matchBuySet.add(temp);
			matchBuySetSize += temp.getQuantity();
		}
	}
	/**
	 * move one PQPoint from matchBuySet to buySet
	 */
	private void demoteBuy()
	{
		PQPoint temp = worstMatchingBuy();
		if (temp != null)
		{
			matchBuySet.remove(temp);
			buySet.add(temp);
			matchBuySetSize -= temp.getQuantity();
		}
	}
	/**
	 * move one PQPoint from Sellset to matchSellSet
	 */
	private void promoteSell()
	{
		PQPoint temp = bestSell();
		if (temp != null)
		{
			sellSet.remove(temp);
			matchSellSet.add(temp);
			matchSellSetSize += Math.abs(temp.getQuantity());
		}
	}
	/**
	 * move one PQPoint from matchSellSet to SellSet
	 */
	private void demoteSell()
	{
		PQPoint temp = worstMatchingSell();
		if (temp != null)
		{
			matchSellSet.remove(temp);
			sellSet.add(temp);
			matchSellSetSize -= Math.abs(temp.getQuantity());
		}
	}
	final void debug(String message)
	{
		if (!DEBUG)  return;

		log(4,message);

	}
	final void debugn(String message)
	{
		if (!DEBUG) return;
		log(4,message);


	}


	public static final int P_BUY = 0;
	public static final int P_SELL = 1;
	public static final int P_MB = 2;
	public static final int P_MS = 3;
	public void logSets()
	{
		String s = printSet(0) + printSet(1) + printSet(2) + printSet(3);
//		if (shouldLog(Log.INFO)) {
//			log(Log.INFO,"FourHeap::logSets, "+s);
//		}
		log.log(Log.INFO, "    [[" + mktID + "]] " + "FourHeap::logSets::" + s);
	}
	private String printSet(int t)
	{
		SortedSet S = null;
		String s = new String("");
		if (P_BUY == t)
		{
			s = s+ "Buys: ";
			S = buySet;
		}
		else if (P_SELL == t)
		{
			s = s+ " Sells: ";
			S = sellSet;
		}
		else if (P_MB == t)
		{
			s = s+" MB: ";
			s = s+"  size: "+matchBuySetSize+" ";
			S = matchBuySet;
		}
		else if (P_MS == t)
		{
			s = s+" MS: ";
			s = s+"  size: "+matchSellSetSize+" ";
			S = matchSellSet;
		}
		TreeSet T = (TreeSet)S;
		Iterator I;
		PQPoint P;
		I = T.iterator();
		while(I.hasNext())
		{
			P = (PQPoint)I.next();
			s = s+P.getAgentID()+":"+P.toString();
		}
		//s = s+'\n';
		//System.out.println(s);
		return s;
	}
}
