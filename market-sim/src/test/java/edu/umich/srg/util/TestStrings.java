package edu.umich.srg.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.experimental.theories.ParametersSuppliedBy;

@ParametersSuppliedBy(TestStringsSupplier.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface TestStrings {
	String[] value();
}