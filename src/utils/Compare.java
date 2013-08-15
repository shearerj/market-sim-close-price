package utils;

import java.util.Comparator;

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
	 * Only works if E implements Comparable<E>
	 * @return
	 */
	public static <E extends Comparable<? super E>> Comparator<E> naturalOrder() {
		return new CompComparator<E>();
	}
	
	private static class CompComparator<E extends Comparable<? super E>> implements Comparator<E> {
		@Override
		public int compare(E o1, E o2) {
			return o1.compareTo(o2);
		}
	}

}
