package bdc;

import bdc.Type.FieldType;

public class ValuePrimitive extends Value {

	private final Object value;
	private final boolean isConstant;

	public ValuePrimitive(final FieldType integer, final boolean isConstant, final Object value) {
		super(integer);
		this.isConstant = isConstant;
		this.value = value;
	}

	@Override
	public Object getConstant() {
		if (isConstant()) {
			return this.value;
		} else {
			throw new IllegalStateException();
		}
	}

	@Override
	public boolean isConstant() {
		return this.isConstant;
	}

}
