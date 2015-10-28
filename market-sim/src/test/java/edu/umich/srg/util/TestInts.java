package edu.umich.srg.util;

import org.junit.experimental.theories.ParametersSuppliedBy;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@ParametersSuppliedBy(TestIntsSupplier.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface TestInts {
  int[] value();
}
