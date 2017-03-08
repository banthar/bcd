package bdc;

public class Value {

	private final Object value;
	private final boolean isConstant;

	public Value(final Object value, final boolean isConstant) {
		this.value = value;
		this.isConstant = isConstant;
	}

	public static Value integer(final int n) {
		return new Value(n, true);
	}

	public static Value unknown() {
		return new Value(null, false);
	}

	public boolean isConstant() {
		return this.isConstant;
	}

	public Object getConstant() {
		if (this.isConstant) {
			return this.value;
		} else {
			throw new IllegalStateException();
		}
	}

	public static Value of(final Object value) {
		return new Value(value, true);
	}

}
