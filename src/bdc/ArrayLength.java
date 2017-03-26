package bdc;

import java.util.Map;

public class ArrayLength extends PureOperation {

	public ArrayLength() {
		super(Type.integer());
	}

	@Override
	protected Value computeSingleOutput(final Map<PortId, ? extends Value> values) {
		throw new IllegalStateException();
	}
}
