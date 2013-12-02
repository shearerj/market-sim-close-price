package data;

/**
 * The observation of a single player. It's generated in the player class.
 * 
 * @author erik
 * 
 */
public class PlayerObservation {

	public final String role;
	public final String strategy;
	public final double payoff;
	
	public PlayerObservation(String role, String strategy, double utility) {
		this.role = role;
		this.strategy = strategy;
		this.payoff = utility;
	}
	
}
