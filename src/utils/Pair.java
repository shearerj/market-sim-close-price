package utils;

/**
 * Simple immutable pair implementation
 * 
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

	@Override
	public int hashCode() {
		return (left == null ? 0 : left.hashCode())
				^ (right == null ? 0 : right.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Pair)) return false;
		Pair<?, ?> other = (Pair<?, ?>) obj;
		return ((left == null && other.left == null) || left.equals(other.left))
				&& ((right == null && other.right == null) || right.equals(other.right));
	}

	@Override
	public String toString() {
		return "<" + left + ", " + right + ">";
	}

}
