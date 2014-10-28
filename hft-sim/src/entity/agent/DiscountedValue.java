package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

/**
 * This class represents any value that needs to be stored at all discount
 * values for the simulation. Currently it's only used for background agent
 * surplus.
 * 
 * @author erik
 * 
 */
class DiscountedValue {

	private Map<Double, Double> values;
	
	private DiscountedValue(Iterable<Double> discountFactors) {
		checkArgument(!Iterables.isEmpty(discountFactors));
		values = Maps.newLinkedHashMap();
		for (double discount : discountFactors)
			values.put(discount, 0d);
	}
	
	public static DiscountedValue create(Iterable<Double> discountFactors) {
		return new DiscountedValue(discountFactors);
	}
	
	public void addValue(Number value, double discountTime) {
		for (Entry<Double, Double> e : values.entrySet())
			e.setValue(e.getValue() + Math.exp(-e.getKey() * discountTime) * value.doubleValue());
	}
	
	public Set<Entry<Double, Double>> getValues() {
		return Collections.unmodifiableMap(values).entrySet();
	}
	
}
