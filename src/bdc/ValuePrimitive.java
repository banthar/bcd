package bdc;

import bdc.Type.FieldType;

public class ValuePrimitive extends Value {

	private final Object value;

	public ValuePrimitive(final FieldType integer, final boolean isConstant, final Object value) {
		super(integer, isConstant);
		this.value = value;
	}

	public Object getConstant() {
		if (isConstant()) {
			return this.value;
		} else {
			throw new IllegalStateException();
		}
	}

}
