package bdc;

import java.util.Map;

import bdc.Type.PrimitiveType;

public class ExtractElement extends PureOperation {

	public ExtractElement(final PrimitiveType elementType) {
		super(elementType);
	}

	@Override
	protected Value computeSingleOutput(final Map<PortId, ? extends Value> values) {
		if (!values.get(PortId.arg(0)).isConstant()) {
			return Value.unknown(getType());
		}
		final ValueArray array = (ValueArray) values.get(PortId.arg(0));
		final Value index = values.get(PortId.arg(1));
		return array.extractElement(index);
	}

}
