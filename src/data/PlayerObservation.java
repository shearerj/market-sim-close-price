package data;

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
