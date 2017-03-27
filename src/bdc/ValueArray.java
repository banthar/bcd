package bdc;

import java.util.Arrays;

import bdc.Type.FieldType;

public class ValueArray extends Value {

	private final FieldType elementType;
	private final Value length;
	private final Value[] elements;

	public ValueArray(final FieldType elementType, final boolean isConstant, final Value length,
			final Value[] elements) {
		super(Type.array(elementType), isConstant);
		this.elementType = elementType;
		this.length = length;
		this.elements = elements;
	}

	public ValueArray(final FieldType elementType, final boolean isConstant, final Value length) {
		super(Type.array(elementType), isConstant);
		this.elementType = elementType;
		this.length = length;
		if (length.isConstant()) {
			this.elements = new Value[(Integer) length.getConstant()];
			Arrays.fill(this.elements, Value.integer(0));
		} else {
			this.elements = null;
		}
	}

	public ValueArray(final FieldType elementType, final boolean isConstant, final Object[] value) {
		super(Type.array(elementType), isConstant);
		this.elementType = elementType;
		this.length = Value.integer(value.length);
		this.elements = new Value[value.length];
		for (int i = 0; i < value.length; i++) {
			this.elements[i] = Value.of(elementType, value[i]);
		}
	}

	@Override
	public boolean isConstant() {
		if (!this.length.isConstant()) {
			return false;
		}
		for (final Value element : this.elements) {
			if (!element.isConstant()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Object[] getConstant() {
		final Object[] value = new Object[(Integer) this.length.getConstant()];
		for (int i = 0; i < value.length; i++) {
			value[i] = this.elements[i].getConstant();
		}
		return value;
	}

	public Value getLength() {
		return this.length;
	}

	public Value insertElement(final Value index, final Value value) {
		if (!this.length.isConstant() || !index.isConstant() || !value.isConstant()) {
			return Value.unknown(getType());
		}
		if (this.length.getInt() <= index.getInt()) {
			throw new IllegalStateException("Index out of bounds: " + this.length + " " + index);
		}
		final Value[] newElements = this.elements.clone();
		newElements[(Integer) index.getConstant()] = value;
		return new ValueArray(this.elementType, isConstant(), this.length, newElements);
	}

	public Value extractElement(final Value index) {
		if (index.isConstant() && this.length.isConstant()) {
			if (this.length.getInt() <= index.getInt()) {
				throw new IllegalStateException("Index out of bounds: " + this.length + " " + index);
			}
			return Value.of(this.elementType, getConstant()[(Integer) index.getConstant()]);
		} else {
			return Value.unknown(this.elementType);
		}
	}

	@Override
	public String toString() {
		return "ValueArray(" + getType() + ", " + this.length + ")";
	}
}
