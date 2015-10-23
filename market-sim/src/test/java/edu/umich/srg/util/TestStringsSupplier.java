package edu.umich.srg.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.experimental.theories.ParameterSignature;
import org.junit.experimental.theories.ParameterSupplier;
import org.junit.experimental.theories.PotentialAssignment;

public class TestStringsSupplier extends ParameterSupplier {
	@Override public List<PotentialAssignment> getValueSources(ParameterSignature sig) {
		TestStrings testStrings = sig.getAnnotation(TestStrings.class);
		String[] strings = testStrings.value();
		String name = Arrays.toString(strings);
		List<PotentialAssignment> list = new ArrayList<>(strings.length);
		for (String i : strings)
			list.add(PotentialAssignment.forValue(name, i));
		return list;
	}
}