package edu.umich.srg.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.experimental.theories.ParametersSuppliedBy;

@ParametersSuppliedBy(TestIntsSupplier.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface TestInts {
	int[] value();
}