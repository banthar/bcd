package bdc;

import java.util.Map;

import bdc.ConstantPool.ClassReference;
import bdc.Type.ReferenceType;

public class NewInstance extends PureOperation {

	public NewInstance(final ClassReference classReference) {
		super(classReference.getType());
	}

	@Override
	protected Value computeSingleOutput(final Map<PortId, ? extends Value> values) {
		if (!values.isEmpty()) {
			throw new IllegalStateException();
		}
		return Value.object(getType());
	}

	@Override
	public ReferenceType getType() {
		return (ReferenceType) super.getType();
	}

}
