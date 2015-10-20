package edu.umich.srg.util;

public interface Functions {

	@FunctionalInterface
	interface TriFunction<A, B, C, R> {

		R apply(A a, B b, C c);

	}

}
