package bdc;

import java.util.Map;

public class ArrayLength extends PureOperation {

	public ArrayLength() {
		super(Type.integer());
	}

	@Override
	protected Value computeSingleOutput(final Map<PortId, ? extends Value> values) {
		final ValueArray array = (ValueArray) values.get(PortId.arg(0));
		return array.getLength();
	}
}
