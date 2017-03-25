package bdc;

import bdc.Type.FieldType;

public class Value {

	private final Object value;
	private final FieldType type;
	private final boolean isConstant;

	public Value(final FieldType type, final boolean isConstant, final Object value) {
		this.type = type;
		this.value = value;
		this.isConstant = isConstant;
	}

	public static Value integer(final int n) {
		return new Value(Type.integer(), true, n);
	}

	public static Value unknown() {
		return new Value(Type.getUnknown(), false, null);
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

	public static Value of(final FieldType type, final Object value) {
		return new Value(type, true, value);
	}

	@Override
	public String toString() {
		if (this.isConstant) {
			return this.value == null ? "null" : this.value.getClass().getSimpleName() + "(" + this.value + ")";
		} else {
			return "unknown";
		}
	}

	public FieldType getType() {
		return this.type;
	}
}
