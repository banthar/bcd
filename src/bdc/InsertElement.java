package bdc;

import java.util.Map;

import bdc.Type.PrimitiveType;

public class InsertElement extends PureOperation {

	public InsertElement(final PrimitiveType elementType) {
		super(Type.array(elementType));
	}

	@Override
	protected Value computeSingleOutput(final Map<PortId, ? extends Value> values) {
		if (!values.get(PortId.arg(0)).isConstant()) {
			return Value.unknown(getType());
		}
		final ValueArray array = (ValueArray) values.get(PortId.arg(0));
		final Value index = values.get(PortId.arg(1));
		final Value value = values.get(PortId.arg(2));
		return array.insertElement(index, value);
	}
}
