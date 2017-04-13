package bdc;

import java.util.Map;

import bdc.ConstantPool.FieldReference;
import bdc.Type.ArrayType;
import bdc.Type.FieldType;
import bdc.Type.PrimitiveType;
import bdc.Type.ReferenceType;

public abstract class Value {

	private final FieldType type;

	public Value(final FieldType type) {
		this.type = type;
	}

	public static Value integer(final int n) {
		return new ValuePrimitive(Type.integer(), true, n);
	}

	public static Value unknown(final FieldType type) {
		return new Value(type) {

			@Override
			public boolean isConstant() {
				return false;
			}

			@Override
			public Object getConstant() {
				throw new IllegalStateException();
			}

			@Override
			public String toString() {
				return "unknown";
			}
		};
	}

	public abstract Object getConstant();

	public abstract boolean isConstant();

	public static Value of(final FieldType type, final Object value) {
		if (type instanceof ArrayType) {
			return new ValueArray(((ArrayType) type).getElementType(), (Object[]) value);
		} else if (type instanceof PrimitiveType) {
			return new ValuePrimitive(type, true, value);
		} else if (type instanceof ReferenceType) {
			return new ValueObject(type, (Map<FieldReference, ?>) value);
		} else {
			throw new IllegalStateException("Unsupported type: " + type);
		}
	}

	public FieldType getType() {
		return this.type;
	}

	public static ValueArray array(final FieldType elementType, final Value length) {
		return new ValueArray(elementType, length);
	}

	public static ValueObject object(final ReferenceType type) {
		return new ValueObject(type);
	}

	public int getInt() {
		return (Integer) getConstant();
	}

}
