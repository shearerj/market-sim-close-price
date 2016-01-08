package edu.umich.srg.util;

public interface Functions {

  @FunctionalInterface
  interface TriFunction<A, B, C, R> {

    R apply(A input1, B input2, C input3);

  }

}
