package utils;

import com.google.common.base.Objects;

/**
 * Simple immutable pair implementation
 * 
 * @author drhurd
 * 
 * @param <A>
 * @param <B>
 */
// FIXME Factory Pattern
public class Pair<A, B> {

	private final A left;
	private final B right;

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
		return Objects.hashCode(left, right);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Pair)) return false;
		Pair<?, ?> other = (Pair<?, ?>) obj;
		return Objects.equal(left, other.left) && Objects.equal(right, other.right);
	}

	@Override
	public String toString() {
		return "<" + left + ", " + right + ">";
	}

}
