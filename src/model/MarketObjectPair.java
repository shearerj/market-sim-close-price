package model;

/**
 * Pair of (market type, object). Used to specify configuration in MarketModel
 * objects.
 * 
 * @author ewah
 */
public class MarketObjectPair {

	  private final String left;
	  private final Object right;

	  public MarketObjectPair(String left, Object right) {
		  this.left = left;
		  this.right = right;
	  }

	  public String getMarketType() { return left; }

	  public Object getObject() { return right; }

	  @Override
	  public int hashCode() { return left.hashCode() ^ right.hashCode(); }

	  @Override
	  public boolean equals(Object o) {
		  if (o == null) return false;
		  if (!(o instanceof MarketObjectPair)) return false;
		  MarketObjectPair mpo = (MarketObjectPair) o;
		  return this.left.equals(mpo.getMarketType()) &&
		       this.right.equals(mpo.getObject());
	  }
}
