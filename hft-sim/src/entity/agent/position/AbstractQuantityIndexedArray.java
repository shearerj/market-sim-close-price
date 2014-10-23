package entity.agent.position;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static fourheap.Order.OrderType.BUY;

import java.util.Collections;
import java.util.List;

import com.google.common.base.Objects;

import fourheap.Order.OrderType;

abstract class AbstractQuantityIndexedArray<T> implements QuantityIndexedArray<T> {
	private final int offset;
	private final List<T> values;
	
	protected AbstractQuantityIndexedArray(List<T> values) {
		checkArgument(values.size() % 2 == 0, "Number of values must be even");
		this.values = values;
		this.offset = this.values.size() / 2;
	}
	
	protected List<T> getList() {
		return Collections.unmodifiableList(values);
	}

	@Override
	public int getMaxAbsPosition() {
		return offset;
	}
	
	/** Value to return if index goes below position */
	protected abstract T lowerBound();
	
	/** Value to return if index goes above position */
	protected abstract T upperBound();
	
	private int getIndex(int currentPosition, OrderType type) {
		return (checkNotNull(type) == BUY ? offset : offset-1) + currentPosition;	
	}
	
	@Override
	public T getValue(int currentPosition, OrderType type) {
		int index = getIndex(currentPosition, type);
		if (index < 0)
			return lowerBound();
		else if (index >= values.size())
			return upperBound();
		else
			return values.get(index);
	}
	
	@Override
	public void setValue(int currentPosition, OrderType type, T value) {
		int index = getIndex(currentPosition, type);
		if (index >= 0 && index < values.size())
			values.set(index, value);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(values, offset);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj.getClass().equals(this.getClass())))
			return false;
		AbstractQuantityIndexedArray<?> other = (AbstractQuantityIndexedArray<?>) obj;
		return other.offset == this.offset && other.values.equals(values);
	}
	
	@Override
	public String toString() {
		return values.toString();
	}

	private static final long serialVersionUID = 6257743271096714582L;
	
}
