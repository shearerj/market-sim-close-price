package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;
import static entity.market.Price.ZERO;
import static utils.MathUtils.bound;


import java.util.Collections;
import java.util.List;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import entity.market.Price;

public class MockPrivateValue extends PrivateValue {
	
	/**
	 * Mock Private Value to make testing of ZIstrategy easier
	 * 
	 * Initialized to all zero, so private value will equal fundamental (Passed maxPosition)
	 * -OR-
	 * Initialized with a list of values (Does not check if maxPosition is correct)
	 * 
	 * Superclass PrivateValue holds all zero values.
	 * TODO Add protected constructor to PrivateValue that gives access to offset and prices
	 */
	
	private static final long serialVersionUID = 1L;
	
	
	protected final List<Price> test_prices;
	protected final int test_offset;
	
	public MockPrivateValue(){
		super();
		test_offset = 0;
		test_prices = Collections.singletonList(ZERO);
	}
	
	public MockPrivateValue(int maxPosition){
		super();
		checkArgument(maxPosition > 0, "Max Position must be positive");
		test_offset = maxPosition;
		Builder<Price> builder = ImmutableList.builder();
		for(int i = 0; i<2*test_offset+1; i++){
			builder.add(new Price(0));
		}
		test_prices = builder.build();
		
	}
	
	public MockPrivateValue(List<Price> _prices){
		super();
	    test_prices = prices;
		test_offset = (test_prices.size()-1)/2;
		
	}
	
	/**
	 * All methods had to be overridden because PrivateValue.offset and .prices were final
	 * Could not initialize them in a subclass
	 */

	@Override
	public int getMaxAbsPosition() {
		return test_offset;
	}
	
	@Override 
	public Price getValueFromQuantity(int position, int quantity) {
		return new Price(getValueAtPosition(position + quantity).intValue()
				- getValueAtPosition(position).intValue());
	}


	public Price getValueAtPosition(int position) {
		return test_prices.get(bound(position + test_offset, 0, test_prices.size() - 1));
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(test_prices, test_offset);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof MockPrivateValue))
			return false;
		MockPrivateValue other = (MockPrivateValue) obj;
		return other.test_offset == this.test_offset && other.test_prices.equals(test_prices);
	}

	@Override
	public String toString() {
		return test_prices.toString();
	}

}
