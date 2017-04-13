package bdc;

import java.util.Collections;
import java.util.Map;

import bdc.ConstantPool.FieldReference;
import bdc.Type.FieldType;
import bdc.Type.ReferenceType;

public class ValueObject extends Value {

	public ValueObject(final ReferenceType type) {
		super(type);
	}

	public ValueObject(final FieldType type, final Map<FieldReference, ?> value) {
		super(type);
	}

	@Override
	public Map<FieldReference, ?> getConstant() {
		return Collections.emptyMap();
	}

	@Override
	public boolean isConstant() {
		return true;
	}

}
