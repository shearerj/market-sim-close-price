package entity.agent;

import systemmanager.Consts;

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
	
	public double getValueAtDiscount(double discount) {
		for (int i = 0; i < values.length; i++)
			if (Consts.DISCOUNT_FACTORS[i] == discount)
				return values[i];
		throw new IllegalArgumentException("Discount factor not found!");
	}
	
}
