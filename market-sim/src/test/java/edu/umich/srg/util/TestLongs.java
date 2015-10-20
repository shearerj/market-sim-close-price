package edu.umich.srg.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.experimental.theories.ParametersSuppliedBy;

@ParametersSuppliedBy(TestLongsSupplier.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface TestLongs {
	long[] value();
}