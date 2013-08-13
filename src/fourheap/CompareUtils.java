package fourheap;

import java.io.Serializable;
import java.util.Comparator;

public class CompareUtils {

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
	
	public static <T> Comparator<T> naturalOrder() {
		return new CompComparator<T>();
	}
	
	private static class CompComparator<T> implements Comparator<T>, Serializable {
		private static final long serialVersionUID = 6468496103741355158L;

		@SuppressWarnings("unchecked")
		@Override
		public int compare(T t1, T t2) {
			return ((Comparable<T>) t1).compareTo(t2);
		}
	}

}
