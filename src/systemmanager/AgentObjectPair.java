package systemmanager;

/**
 * Pair of (agent type, object string). Used to store assignments from specification
 * file.
 * 
 * @author ewah
 */
public class AgentObjectPair {

	  private final String left;
	  private final Object right;

	  public AgentObjectPair(String left, Object right) {
		  this.left = left;
		  this.right = right;
	  }

	  public String getAgentType() { return left; }

	  public Object getStrategy() { return right; }

	  @Override
	  public int hashCode() { return left.hashCode() ^ right.hashCode(); }

	  @Override
	  public boolean equals(Object o) {
		  if (o == null) return false;
		  if (!(o instanceof AgentObjectPair)) return false;
		  AgentObjectPair mpo = (AgentObjectPair) o;
		  return this.left.equals(mpo.getAgentType()) &&
		       this.right.equals(o);
	  }
}
