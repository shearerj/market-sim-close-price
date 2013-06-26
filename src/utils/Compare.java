package utils;

import java.util.Collection;

public class Compare {

	/**
	 * Returns the maximum of two comparable elements. If the elements are the
	 * same this return the first.
	 */
	public static <T extends Comparable<? super T>> T max(T a, T b) {
		if (a == null)
			return b;
		if (b == null)
			return a;
		return a.compareTo(b) >= 0 ? a : b;
	}

	/**
	 * Returns the minimum of two comparable elements. If the elements are the
	 * same this returns the first.
	 */
	public static <T extends Comparable<? super T>> T min(T a, T b) {
		if (a == null)
			return b;
		if (b == null)
			return a;
		return a.compareTo(b) <= 0 ? a : b;
	}

	/**
	 * Returns the maximum element in a collection. In the presence of a tie it
	 * will return the first element.
	 */
	public static <T extends Comparable<? super T>> T maximum(
			Collection<? extends T> elems) {
		T max = null;
		for (T e : elems)
			if (max == null || (e != null && max.compareTo(e) < 0))
				max = e;
		return max;
	}
	
	/**
	 * Returns the minimum element in a collection. In the presence of a tie it
	 * will return the first element.
	 */
	public static <T extends Comparable<? super T>> T minimum(
			Collection<? extends T> elems) {
		T min = null;
		for (T e : elems)
			if (min == null || (e != null && min.compareTo(e) > 0))
				min = e;
		return min;
	}

}
