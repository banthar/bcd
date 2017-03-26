package bdc;

import java.util.Map;

import bdc.Type.PrimitiveType;

public class ExtractElement extends PureOperation {

	public ExtractElement(final PrimitiveType elementType) {
		super(Type.array(elementType));
	}

	@Override
	protected Value computeSingleOutput(final Map<PortId, ? extends Value> values) {
		throw new IllegalStateException();
	}

}
