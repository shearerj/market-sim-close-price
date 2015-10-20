package edu.umich.srg.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.experimental.theories.ParametersSuppliedBy;

@ParametersSuppliedBy(TestDoublesSupplier.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface TestDoubles {
	double[] value();
}