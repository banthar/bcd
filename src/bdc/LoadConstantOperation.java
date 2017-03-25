package bdc;

import bdc.Type.FieldType;

public class LoadConstantOperation extends PureOperation {

	private final Object value;

	public LoadConstantOperation(final FieldType type, final Object value) {
		super(type, 0);
		this.value = value;
	}

	public Object getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return "LoadConstantOperation(" + getType() + ", " + this.value + ")";
	}

	public Value toValue() {
		return Value.of(getType(), this.value);
	}

}
