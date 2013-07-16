package utils;

/**
 * Simple immutable pair implementation
 * @author drhurd
 *
 * @param <A>
 * @param <B>
 */
public class Pair<A, B> {

	private A left;
	private B right;
	
	public Pair(A left, B right) {
		this.left = left;
		this.right = right;
	}
	
	public A left() {
		return left;
	}

	public B right() {
		return right;
	}
}
