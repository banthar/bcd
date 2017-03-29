package bdc;

import bdc.Type.ArrayType;
import bdc.Type.FieldType;

public abstract class Value {

	private final FieldType type;
	private final boolean isConstant;

	public Value(final FieldType type, final boolean isConstant) {
		this.type = type;
		this.isConstant = isConstant;
	}

	public static Value integer(final int n) {
		return new ValuePrimitive(Type.integer(), true, n);
	}

	public static Value unknown(final FieldType type) {
		return new Value(type, false) {
			@Override
			Object getConstant() {
				throw new IllegalStateException();
			}

			@Override
			public String toString() {
				return "unknown";
			}
		};
	}

	abstract Object getConstant();

	public boolean isConstant() {
		return this.isConstant;
	}

	public static Value of(final FieldType type, final Object value) {
		if (type instanceof ArrayType) {
			return new ValueArray(((ArrayType) type).getElementType(), true, (Object[]) value);
		} else {
			return new ValuePrimitive(type, true, value);
		}
	}

	public FieldType getType() {
		return this.type;
	}

	public static ValueArray array(final FieldType elementType, final Value length) {
		return new ValueArray(elementType, false, length);
	}

	public int getInt() {
		return (Integer) getConstant();
	}
}
