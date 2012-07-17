package activity;


import entity.Agent;
import event.*;

/**
 * Class for executing agent strategies.
 * 
 * @author ewah
 */
public class AgentStrategy extends Activity {

	private Agent ag;

	public AgentStrategy(Agent ag, TimeStamp t) {
		this.ag = ag;
		this.time = t;
	}
	
	public ActivityHashMap execute() {
		return this.ag.agentStrategy(this.time);
	}
	
	public String toString() {
		return new String("AgentStrategy(Agt " + this.ag.getID() + ")");
	}
}
