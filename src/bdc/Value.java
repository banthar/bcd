package bdc;

import java.util.Map;

import bdc.ConstantPool.FieldReference;
import bdc.Type.ArrayType;
import bdc.Type.FieldType;
import bdc.Type.PrimitiveType;
import bdc.Type.ReferenceType;

public abstract class Value {

	private final FieldType type;
	private final Throwable constructed = new Throwable("Constructed at");

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

	public static Value unknownHeapReference() {
		return unknown(Type.unknown());
	}

	public static Value unknownEnvironment() {
		return unknown(Type.unknown());
	}

	public abstract Object getConstant();

	public abstract boolean isConstant();

	public static Value of(final FieldType type, final Object value) {
		if (type instanceof ArrayType) {
			return new ValueArray(((ArrayType) type).getElementType(), (Object[]) value);
		} else if (type instanceof PrimitiveType) {
			return new ValuePrimitive((PrimitiveType) type, true, value);
		} else if (type instanceof ReferenceType) {
			return new ValueObject(type, (Map<FieldReference, ? extends Value>) value);
		} else {
			throw new IllegalStateException("Unsupported type: " + type);
		}
	}

	public static Value zero(final FieldType type) {
		if (type instanceof PrimitiveType) {
			return Value.integer(0);
		} else if (type instanceof ReferenceType) {
			return new Value(type) {

				@Override
				public boolean isConstant() {
					return true;
				}

				@Override
				public Object getConstant() {
					return null;
				}
			};
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

	public <T> T as(final java.lang.Class<T> type) {
		if (type.isInstance(this)) {
			return type.cast(this);
		} else {
			throw new IllegalStateException(this + " is not a " + type.getName(), this.constructed);
		}
	}
}
