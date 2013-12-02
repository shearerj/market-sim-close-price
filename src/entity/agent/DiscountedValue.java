package entity.agent;

import systemmanager.Consts;

/**
 * This class represents any value that needs to be stored at all discount
 * values for the simulation. Currently it's only used for background agent
 * surplus.
 * 
 * @author erik
 * 
 */
class DiscountedValue {

	private double[] values;
	
	public DiscountedValue() {
		values = new double[Consts.DISCOUNT_FACTORS.length];
	}
	
	public static DiscountedValue create() {
		return new DiscountedValue();
	}
	
	public void addValue(double value, double discountTime) {
		for (int i = 0; i < values.length; i++)
			values[i] += Math.exp(-Consts.DISCOUNT_FACTORS[i] * discountTime) * value;
	}
	
	public double[] getDiscountedValues() {
		return values;
	}
	
	/**
	 * This method is very inefficient. Much better to use the array of iterate
	 * if you know which one.
	 * 
	 * @param discount
	 * @return
	 */
	public double getValueAtDiscount(double discount) {
		for (int i = 0; i < values.length; i++)
			if (Consts.DISCOUNT_FACTORS[i] == discount)
				return values[i];
		throw new IllegalArgumentException("Discount factor not found!");
	}
	
}
