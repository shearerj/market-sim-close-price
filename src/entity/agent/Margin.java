package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import utils.Rands;
import fourheap.Order.OrderType;

/**
 * 
 * TODO check EC paper on the multi-quantity margins for ZIP
 * 
 * @author ewah
 *
 */
class Margin implements Serializable, QuantityIndexedValue<Double> {

	private static final long serialVersionUID = -3749423779545857329L;
	
	protected final int offset;
	protected List<Double> values;
	
	/**
	 * @param maxPosition
	 * @param rand
	 * @param a
	 * @param b
	 */
	public Margin(int maxPosition, Random rand, double a, double b) {
		checkArgument(maxPosition > 0, "Max Position must be positive");
		
		// Identical to legacy generation in final output
		this.offset = maxPosition;
		double[] values = new double[maxPosition * 2];
		for (int i = 0; i < values.length; i++)
			values[i] = Rands.nextUniform(rand, a, b) *	(i >= maxPosition ? -1 : 1);
			// margins for buy orders are negative
		
		Builder<Double> builder = ImmutableList.builder();
		for (double value : values)
			builder.add(new Double(value));
		
		this.values = builder.build();
	}
	
	/**
	 * Protected constructor for testing purposes.
	 * 
	 * @param maxPosition
	 * @param values
	 */
	protected Margin(int maxPosition, Collection<Double> values) {
		checkArgument(values.size() == 2*maxPosition, "Incorrect number of entries in list");
		Builder<Double> builder = ImmutableList.builder();
		builder.addAll(values);
		this.values = builder.build();
		offset = maxPosition;
	}
	
	@Override
	public int getMaxAbsPosition() {
		return offset;
	}

	@Override
	public Double getValue(int currentPosition, OrderType type) {
		
		// should return the max margin (in either direction)
		switch (type) {
		case BUY:
			if (currentPosition + offset <= values.size() - 1 &&
					currentPosition + offset >= 0)
				return values.get(currentPosition + offset);
			break;
		case SELL:
			if (currentPosition + offset - 1 <= values.size() - 1 && 
					currentPosition + offset - 1 >= 0)
				return values.get(currentPosition + offset - 1);
			break;
		}
		return 0.0;
	}

	// TODO need to test
	public void setValue(int currentPosition, int quantity, OrderType type,
			double value) {
		switch (type) {
		case BUY:
			if (currentPosition + offset <= values.size() - 1 &&
					currentPosition + offset >= 0)
				values.add(currentPosition + offset, value);
			break;
		case SELL:
			if (currentPosition + offset - 1 <= values.size() - 1 && 
					currentPosition + offset - 1 >= 0)
				values.add(currentPosition + offset - 1, value);
			break;
		}
	}
	
	//FIXME this function is definitely wrong right now
	@Override
	public Double getValueFromQuantity(int currentPosition, int quantity,
			OrderType type) {
		checkArgument(quantity > 0, "Quantity must be positive");
		int value = 0;
		
		switch (type) {
		case BUY:
			for (int i = 0; i < quantity; i++) // FIXME
				value += getValue(currentPosition + i, type).intValue();
			break;
		case SELL:
			for (int i = 0; i < quantity; i++)
				value += getValue(currentPosition - i, type).intValue();
			break;
		}
		return new Double(value);
	}
	
//	private double getMarginAt(int quantity) {
//		if (quantity > 0) {
//			// if buying
//			if (positionBalance >= 0) {
//				// if nonnegative current position, look at next position (+q)
//				return margin.getValueFromQuantity(positionBalance + quantity);
//			} else {
//				// if negative current position, look at current position
//				return margin.getValueFromQuantity(positionBalance);
//			}
//
//		} else if (quantity < 0){
//			// if selling
//			if (positionBalance > 0) {
//				// if positive current position, look at current position
//				return margin.getValueFromQuantity(positionBalance);
//			} else {
//				// if non-positive current position, look at next position (-|q|)
//				return margin.getValueFromQuantity(positionBalance + quantity);
//			}
//
//		} else {
//			// not selling or buying
//			return 0;
//		}
//	}

//	private void setMarginAt(int q, double val) {
//		if (q > 0) {
//			// if buying
//			if (positionBalance >= 0) {
//				// if nonnegative current position, look at next position (+q)
//				margins.setValueByQuantity(positionBalance + q, val);
//			} else {
//				// if negative current position, look at current position
//				margins.setValueByQuantity(positionBalance, val);
//			}
//
//		} else if (q < 0){
//			// if selling
//			if (positionBalance > 0) {
//				// if positive current position, look at current position
//				margins.setValueByQuantity(positionBalance, val);
//			} else {
//				// if non-positive current position, look at next position (-|q|)
//				margins.setValueByQuantity(positionBalance + q, val);
//			}	
//		}
//	}


}
