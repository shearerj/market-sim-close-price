package edu.umich.srg.util;

import java.util.Optional;
import java.util.function.BiFunction;

public class Optionals {

  public static <T, U, R> Optional<R> apply(BiFunction<T, U, R> function, Optional<T> firstArgument,
      Optional<U> secondArgument) {
    return firstArgument.flatMap(f -> secondArgument.map(s -> function.apply(f, s)));
  }

  private Optionals() {}

}
