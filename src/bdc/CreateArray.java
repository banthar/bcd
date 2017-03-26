package bdc;

import java.util.Map;

import bdc.Type.FieldType;

public class CreateArray extends PureOperation {

	public CreateArray(final FieldType elementType) {
		super(Type.array(elementType));
	}

	@Override
	protected Value computeSingleOutput(final Map<PortId, ? extends Value> values) {
		throw new IllegalStateException();
	}

}
