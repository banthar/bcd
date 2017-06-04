package bdc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import bdc.ConstantPool.FieldReference;
import bdc.Type.FieldType;
import bdc.Type.ReferenceType;

public class ValueObject extends Value {

	private final Map<FieldReference, ? extends Value> value;

	public ValueObject(final ReferenceType type) {
		this(type, Collections.emptyMap());
	}

	public ValueObject(final FieldType type, final Map<FieldReference, ? extends Value> value) {
		super(type);
		this.value = value;
	}

	@Override
	public Map<FieldReference, ? extends Value> getConstant() {
		return this.value;
	}

	@Override
	public boolean isConstant() {
		return true;
	}

	public ValueObject put(final FieldReference field, final Value value) {
		final HashMap<FieldReference, Value> map = new HashMap<>(getConstant());
		map.put(field, value);
		return new ValueObject(getType(), Collections.unmodifiableMap(map));
	}

}
