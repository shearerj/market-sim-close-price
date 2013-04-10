package systemmanager;

import entity.Agent;

/**
 * Pair of (agent type, object string). Used to store assignments from specification
 * file.
 * 
 * @author ewah
 */
public class AgentPropsPair {

	  private final String left;
	  private final ObjectProperties right;

	  public AgentPropsPair(String left, ObjectProperties right) {
		  this.left = left;
		  this.right = right;
	  }

	  public String getAgentType() { return left; }

	  public ObjectProperties getProperties() { return right; }

	  @Override
	  public int hashCode() { return left.hashCode() ^ right.hashCode(); }

	  @Override
	  public boolean equals(Object o) {
		  if (this == o) 
			  return true;
		  if (o == null)
			  return false;
		  if (getClass() != o.getClass())
			  return false;
		  if (!(o instanceof AgentPropsPair))
			  return false;
		  final AgentPropsPair apo = (AgentPropsPair) o;
		  return this.left.equals(apo.getAgentType()) &&
		       this.right.equals(apo.getProperties());
	  }
	  
	  @Override
	  public String toString() {
		  return "<" + this.left + "," + this.right + ">";
	  }
}
