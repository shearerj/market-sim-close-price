package utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.Ordering;

public class OrderingsTest {

	@Test
	public void optionalOrderings() {
		Ordering<Optional<Integer>> ord = Orderings.optionalOrdering(Ordering.<Integer> natural());
		assertEquals(Optional.absent(), ord.max(Optional.<Integer> absent(), Optional.<Integer> absent()));
		assertEquals(Optional.of(1), ord.max(Optional.<Integer> absent(), Optional.of(1)));
		assertEquals(Optional.of(2), ord.max(Optional.of(2), Optional.of(1)));
		assertEquals(Optional.absent(), ord.min(Optional.<Integer> absent(), Optional.<Integer> absent()));
		assertEquals(Optional.absent(), ord.min(Optional.<Integer> absent(), Optional.of(1)));
		assertEquals(Optional.of(1), ord.min(Optional.of(2), Optional.of(1)));
	}

}
