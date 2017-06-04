package bdc;

import bdc.Type.PrimitiveType;

public class ValuePrimitive extends Value {

	private final Object value;
	private final boolean isConstant;

	public ValuePrimitive(final PrimitiveType type, final boolean isConstant, final Object value) {
		super(type);
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
