package data;

import systemmanager.Consts.AgentType;


/**
 * Pair of (agent type, object string). Used to store assignments from specification
 * file.
 * 
 * @author ewah
 */
public class AgentPropsPair {

	  private final AgentType left;
	  private final EntityProperties right;

	  public AgentPropsPair(AgentType left, EntityProperties right) {
		  this.left = left;
		  this.right = right;
	  }

	  public AgentType getAgentType() { return left; }

	  public EntityProperties getProperties() { return right; }

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
