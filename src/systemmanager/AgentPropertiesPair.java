package systemmanager;

/**
 * Pair of (agent type, object string). Used to store assignments from specification
 * file.
 * 
 * @author ewah
 */
public class AgentPropertiesPair {

	  private final String left;
	  private final ObjectProperties right;

	  public AgentPropertiesPair(String left, ObjectProperties right) {
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
		  if (!(o instanceof AgentPropertiesPair))
			  return false;
		  final AgentPropertiesPair apo = (AgentPropertiesPair) o;
		  return this.left.equals(apo.getAgentType()) &&
		       this.right.equals(apo.getProperties());
	  }
}
