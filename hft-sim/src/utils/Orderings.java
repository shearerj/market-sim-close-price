package utils;

import com.google.common.base.Optional;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Booleans;

public class Orderings {

	public static <T extends Comparable<? super T>> Ordering<Optional<T>> optionalOrdering(final Ordering<T> ordering) {
		return new Ordering<Optional<T>>() {
			@Override
			public int compare(Optional<T> arg0, Optional<T> arg1) {
				if (arg0.isPresent() && arg1.isPresent()) {
					return ordering.compare(arg0.get(), arg1.get());
				}

				return Booleans.compare(arg0.isPresent(), arg1.isPresent());
			}
		};
	}
	
}
