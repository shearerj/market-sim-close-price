package props;

public abstract class Value<T> {
	private T value;

	public final void set(T value) {
		this.value = value;
	}

	public final T get() {
		return value;
	}

	@Override
	public String toString() {
		return value.toString();
	}
	
	
}
