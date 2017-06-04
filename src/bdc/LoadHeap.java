package bdc;

import bdc.Type.FieldType;

public class LoadHeap implements NodeOperation {

	private final FieldType type;

	public LoadHeap(final FieldType type) {
		this.type = type;
	}

	@Override
	public Value getValue(final Node node, final PortId portId) {
		if (portId.equals(PortId.arg(0))) {
			return Value.unknown(this.type);
		} else {
			throw new IllegalStateException();
		}
	}

}
